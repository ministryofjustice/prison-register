alter table email_address drop constraint if exists email_unique_constraint;
alter table phone_number drop constraint if exists phone_number_unique_constraint;
alter table web_address drop constraint if exists web_address_unique_constraint;

-- Keep the original row for the first owner, prioritising agency-visible contact IDs.
create temporary table retained_contact_owner
(
    contact_table text not null,
    contact_id integer not null,
    primary key (contact_table, contact_id)
) on commit drop;

do $$
declare
    contact_table text;
    sequence_name text;
    link record;
    contact record;
    retained integer;
    copied_id integer;
begin
    foreach contact_table in array array['email_address', 'phone_number', 'web_address']
    loop
        execute format('lock table %I in access exclusive mode', contact_table);
        sequence_name := pg_get_serial_sequence(contact_table, 'id');
        execute format(
            'select setval(%L, greatest(coalesce(max(id), 1), coalesce(pg_sequence_last_value(%L::regclass), 1)), true) from %I',
            sequence_name, sequence_name, contact_table
        );
    end loop;

    for link in
        select * from (values
            (1, 'email_address', 'court_to_email_address', 'email_address_id', 'court_id'),
            (2, 'email_address', 'probation_office_to_email_address', 'email_address_id', 'probation_office_id'),
            (3, 'email_address', 'police_custody_suite_to_email_address', 'email_address_id', 'police_custody_suite_id'),
            (4, 'email_address', 'approved_premises_to_email_address', 'email_address_id', 'approved_premises_id'),
            (5, 'email_address', 'agency_to_email_address', 'email_address_id', 'agency_id'),
            (6, 'phone_number', 'court_to_phone', 'phone_id', 'court_id'),
            (7, 'phone_number', 'hospital_to_phone', 'phone_id', 'hospital_id'),
            (8, 'phone_number', 'probation_office_to_phone', 'phone_id', 'probation_office_id'),
            (9, 'phone_number', 'police_custody_suite_to_phone', 'phone_id', 'police_custody_suite_id'),
            (10, 'phone_number', 'approved_premises_to_phone', 'phone_id', 'approved_premises_id'),
            (11, 'phone_number', 'agency_to_phone', 'phone_id', 'agency_id'),
            (12, 'email_address', 'contact_details_to_email_address', 'email_address_id', 'contact_details_id'),
            (13, 'phone_number', 'contact_details_to_phone_number', 'phone_number_id', 'contact_details_id'),
            (14, 'web_address', 'contact_details_to_web_address', 'web_address_id', 'contact_details_id')
        ) as links(priority, contact_table, link_table, contact_column, owner_column)
        order by priority
    loop
        execute format('lock table %I in access exclusive mode', link.link_table);
        for contact in execute format(
            'select ctid as link_row, %I as id from %I order by %I, ctid',
            link.contact_column, link.link_table, link.owner_column
        )
        loop
            insert into retained_contact_owner values (link.contact_table, contact.id)
                on conflict do nothing;
            get diagnostics retained = row_count;
            if retained = 0 then
                execute format(
                    'insert into %I (value) select value from %I where id = $1 returning id',
                    link.contact_table, link.contact_table
                ) into copied_id using contact.id;
                execute format(
                    'update %I set %I = $1 where ctid = $2',
                    link.link_table, link.contact_column
                ) using copied_id, contact.link_row;
            end if;
        end loop;
    end loop;
end $$;

alter table contact_details_to_email_address
    add constraint contact_details_email_owner_unique unique (email_address_id);
alter table contact_details_to_phone_number
    add constraint contact_details_phone_owner_unique unique (phone_number_id);
alter table contact_details_to_web_address
    add constraint contact_details_web_owner_unique unique (web_address_id);

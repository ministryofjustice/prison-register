alter table prison_type
    drop constraint prison_type_prison_fk,
    add constraint prison_type_prison_fk
        foreign key (prison_id)
            references prison (prison_id)
            on delete cascade;


alter table prison_operator
    drop constraint prison_operator_operator_id_fkey,
    add constraint prison_operator_operator_id_fkey
        foreign key (prison_id)
            references prison (prison_id)
            on delete cascade;
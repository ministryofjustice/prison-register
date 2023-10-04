CREATE TEMP TABLE tmp_email_addresses(id serial NOT NULL,prison_id VARCHAR(6)  NOT NULL,email_address varchar(100) NOT NULL, purpose_type varchar(40)  NOT NULL);

INSERT INTO tmp_email_addresses(prison_id, email_address, purpose_type)
    select prison_id, email_address, 'VIDEO_LINK_CONFERENCING'
    from videolink_conferencing_centre GROUP BY prison_id, email_address;

INSERT INTO tmp_email_addresses(prison_id, email_address, purpose_type)
    select prison_id, email_address, 'OFFENDER_MANAGEMENT_UNIT'
    from offender_management_unit GROUP BY prison_id, email_address;

INSERT INTO email_address(value)
    select email_address
    from tmp_email_addresses GROUP BY email_address;

INSERT INTO contact_details(prison_id, purpose_type)
    select prison_id, purpose_type
    from tmp_email_addresses ORDER BY ID;

INSERT INTO contact_details_to_email_address(contact_details_id,email_address_id)
            select sb.cd_id,sb.ea_id from (select cd.id as cd_id,ea.id as ea_id from tmp_email_addresses tmp
                                                      join contact_details cd on cd.prison_id=tmp.prison_id and cd.purpose_type=tmp.purpose_type
                                                      join email_address ea on ea.value = tmp.email_address
                                            ) as sb;

DROP TABLE offender_management_unit, videolink_conferencing_centre;


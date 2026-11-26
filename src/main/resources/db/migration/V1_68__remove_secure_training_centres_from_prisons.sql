-- STC2 (Rainsbrook) and STC3 (Oakhill) are secure training centres, not prisons.
-- NOMIS holds them as agency type STC and they are synced into the agency table as SECURE_TRAINING_CENTRE.
-- V1_20 also added them to the prison table, so each appears twice in all_agencies and in the prison lists.
-- Remove the prison copies and everything attached to them. The agency records are left as they are.

with removed as (
    delete from contact_details_to_email_address
    where contact_details_id in (select id from contact_details where prison_id in ('STC2', 'STC3'))
    returning email_address_id
)
delete from email_address where id in (select email_address_id from removed);

with removed as (
    delete from contact_details_to_phone_number
    where contact_details_id in (select id from contact_details where prison_id in ('STC2', 'STC3'))
    returning phone_number_id
)
delete from phone_number where id in (select phone_number_id from removed);

with removed as (
    delete from contact_details_to_web_address
    where contact_details_id in (select id from contact_details where prison_id in ('STC2', 'STC3'))
    returning web_address_id
)
delete from web_address where id in (select web_address_id from removed);

delete from contact_details where prison_id in ('STC2', 'STC3');
delete from address where prison_id in ('STC2', 'STC3');
delete from prison_category where prison_id in ('STC2', 'STC3');
delete from prison_type where prison_id in ('STC2', 'STC3');
delete from prison_operator where prison_id in ('STC2', 'STC3');
delete from offender_management_unit_archive where prison_id in ('STC2', 'STC3');
delete from videolink_conferencing_centre_archive where prison_id in ('STC2', 'STC3');
delete from prison where prison_id in ('STC2', 'STC3');

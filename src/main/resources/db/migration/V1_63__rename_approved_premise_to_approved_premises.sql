alter table approved_premise
    rename to approved_premises;

alter table approved_premises
    rename column approved_premise_id to approved_premises_id;

alter table approved_premises
    rename constraint approved_premise_area_fk to approved_premises_area_fk;

alter table approved_premises
    rename constraint approved_premise_geographical_area_fk to approved_premises_geographical_area_fk;

alter table approved_premises
    rename constraint approved_premise_region_fk to approved_premises_region_fk;

alter table approved_premises
    rename constraint approved_premise_local_authority_fk to approved_premises_local_authority_fk;

alter table approved_premises
    rename constraint approved_premise_payroll_region_fk to approved_premises_payroll_region_fk;


alter table approved_premise_to_agency_address
    rename to approved_premises_to_agency_address;

alter table approved_premises_to_agency_address
    rename column approved_premise_id to approved_premises_id;

alter table approved_premises_to_agency_address
    rename constraint approved_premise_to_agency_address_to_approved_premise_fk to approved_premises_to_agency_address_to_approved_premises_fk;

alter table approved_premises_to_agency_address
    rename constraint approved_premise_to_agency_address_to_agency_address_fk to approved_premises_to_agency_address_to_agency_address_fk;

alter index approved_premise_to_agency_address_idx rename to approved_premises_to_agency_address_idx;


alter table approved_premise_to_phone
    rename to approved_premises_to_phone;

alter table approved_premises_to_phone
    rename column approved_premise_id to approved_premises_id;

alter table approved_premises_to_phone
    rename constraint approved_premise_to_phone_to_approved_premise_fk to approved_premises_to_phone_to_approved_premises_fk;

alter table approved_premises_to_phone
    rename constraint approved_premise_to_phone_to_phone_fk to approved_premises_to_phone_to_phone_fk;

alter index approved_premise_to_phone_idx rename to approved_premises_to_phone_idx;


alter table approved_premise_to_email_address
    rename to approved_premises_to_email_address;

alter table approved_premises_to_email_address
    rename column approved_premise_id to approved_premises_id;

alter table approved_premises_to_email_address
    rename constraint approved_premise_to_email_address_to_approved_premise_fk to approved_premises_to_email_address_to_approved_premises_fk;

alter table approved_premises_to_email_address
    rename constraint approved_premise_to_email_address_to_email_address_fk to approved_premises_to_email_address_to_email_address_fk;

alter index approved_premise_to_email_address_idx rename to approved_premises_to_email_address_idx;

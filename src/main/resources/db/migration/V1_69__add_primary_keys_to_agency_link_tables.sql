-- MAPB-1197: give the 17 tables that link agencies to their addresses, phone numbers and email addresses a
-- primary key. HMPPS Data Hub cannot ingest a table without one, and the key also stops the same link being
-- stored twice.
--
-- Before this was written, every table was checked in dev, preprod and prod: none had a primary key and none
-- held a duplicate (owner id, linked id) pair. The delete before each key is a safeguard in case a duplicate
-- appears before this runs; duplicate rows are identical, so it keeps one of each pair and loses nothing.
--
-- Each table already has a non-unique index on the same two columns, in the same order (<table>_idx). The
-- primary key brings its own unique index, so the old one is dropped.

delete from court_to_agency_address a using court_to_agency_address b
 where a.ctid > b.ctid and a.court_id = b.court_id and a.agency_address_id = b.agency_address_id;
alter table court_to_agency_address add constraint court_to_agency_address_pkey primary key (court_id, agency_address_id);
drop index if exists court_to_agency_address_idx;

delete from court_to_phone a using court_to_phone b
 where a.ctid > b.ctid and a.court_id = b.court_id and a.phone_id = b.phone_id;
alter table court_to_phone add constraint court_to_phone_pkey primary key (court_id, phone_id);
drop index if exists court_to_phone_idx;

delete from court_to_email_address a using court_to_email_address b
 where a.ctid > b.ctid and a.court_id = b.court_id and a.email_address_id = b.email_address_id;
alter table court_to_email_address add constraint court_to_email_address_pkey primary key (court_id, email_address_id);
drop index if exists court_to_email_address_idx;

delete from hospital_to_agency_address a using hospital_to_agency_address b
 where a.ctid > b.ctid and a.hospital_id = b.hospital_id and a.agency_address_id = b.agency_address_id;
alter table hospital_to_agency_address add constraint hospital_to_agency_address_pkey primary key (hospital_id, agency_address_id);
drop index if exists hospital_to_agency_address_idx;

delete from hospital_to_phone a using hospital_to_phone b
 where a.ctid > b.ctid and a.hospital_id = b.hospital_id and a.phone_id = b.phone_id;
alter table hospital_to_phone add constraint hospital_to_phone_pkey primary key (hospital_id, phone_id);
drop index if exists hospital_to_phone_idx;

delete from probation_office_to_agency_address a using probation_office_to_agency_address b
 where a.ctid > b.ctid and a.probation_office_id = b.probation_office_id and a.agency_address_id = b.agency_address_id;
alter table probation_office_to_agency_address add constraint probation_office_to_agency_address_pkey primary key (probation_office_id, agency_address_id);
drop index if exists probation_office_to_agency_address_idx;

delete from probation_office_to_phone a using probation_office_to_phone b
 where a.ctid > b.ctid and a.probation_office_id = b.probation_office_id and a.phone_id = b.phone_id;
alter table probation_office_to_phone add constraint probation_office_to_phone_pkey primary key (probation_office_id, phone_id);
drop index if exists probation_office_to_phone_idx;

delete from probation_office_to_email_address a using probation_office_to_email_address b
 where a.ctid > b.ctid and a.probation_office_id = b.probation_office_id and a.email_address_id = b.email_address_id;
alter table probation_office_to_email_address add constraint probation_office_to_email_address_pkey primary key (probation_office_id, email_address_id);
drop index if exists probation_office_to_email_address_idx;

delete from police_custody_suite_to_agency_address a using police_custody_suite_to_agency_address b
 where a.ctid > b.ctid and a.police_custody_suite_id = b.police_custody_suite_id and a.agency_address_id = b.agency_address_id;
alter table police_custody_suite_to_agency_address add constraint police_custody_suite_to_agency_address_pkey primary key (police_custody_suite_id, agency_address_id);
drop index if exists police_custody_suite_to_agency_address_idx;

delete from police_custody_suite_to_phone a using police_custody_suite_to_phone b
 where a.ctid > b.ctid and a.police_custody_suite_id = b.police_custody_suite_id and a.phone_id = b.phone_id;
alter table police_custody_suite_to_phone add constraint police_custody_suite_to_phone_pkey primary key (police_custody_suite_id, phone_id);
drop index if exists police_custody_suite_to_phone_idx;

delete from police_custody_suite_to_email_address a using police_custody_suite_to_email_address b
 where a.ctid > b.ctid and a.police_custody_suite_id = b.police_custody_suite_id and a.email_address_id = b.email_address_id;
alter table police_custody_suite_to_email_address add constraint police_custody_suite_to_email_address_pkey primary key (police_custody_suite_id, email_address_id);
drop index if exists police_custody_suite_to_email_address_idx;

delete from approved_premises_to_agency_address a using approved_premises_to_agency_address b
 where a.ctid > b.ctid and a.approved_premises_id = b.approved_premises_id and a.agency_address_id = b.agency_address_id;
alter table approved_premises_to_agency_address add constraint approved_premises_to_agency_address_pkey primary key (approved_premises_id, agency_address_id);
drop index if exists approved_premises_to_agency_address_idx;

delete from approved_premises_to_phone a using approved_premises_to_phone b
 where a.ctid > b.ctid and a.approved_premises_id = b.approved_premises_id and a.phone_id = b.phone_id;
alter table approved_premises_to_phone add constraint approved_premises_to_phone_pkey primary key (approved_premises_id, phone_id);
drop index if exists approved_premises_to_phone_idx;

delete from approved_premises_to_email_address a using approved_premises_to_email_address b
 where a.ctid > b.ctid and a.approved_premises_id = b.approved_premises_id and a.email_address_id = b.email_address_id;
alter table approved_premises_to_email_address add constraint approved_premises_to_email_address_pkey primary key (approved_premises_id, email_address_id);
drop index if exists approved_premises_to_email_address_idx;

delete from agency_to_agency_address a using agency_to_agency_address b
 where a.ctid > b.ctid and a.agency_id = b.agency_id and a.agency_address_id = b.agency_address_id;
alter table agency_to_agency_address add constraint agency_to_agency_address_pkey primary key (agency_id, agency_address_id);
drop index if exists agency_to_agency_address_idx;

delete from agency_to_phone a using agency_to_phone b
 where a.ctid > b.ctid and a.agency_id = b.agency_id and a.phone_id = b.phone_id;
alter table agency_to_phone add constraint agency_to_phone_pkey primary key (agency_id, phone_id);
drop index if exists agency_to_phone_idx;

delete from agency_to_email_address a using agency_to_email_address b
 where a.ctid > b.ctid and a.agency_id = b.agency_id and a.email_address_id = b.email_address_id;
alter table agency_to_email_address add constraint agency_to_email_address_pkey primary key (agency_id, email_address_id);
drop index if exists agency_to_email_address_idx;

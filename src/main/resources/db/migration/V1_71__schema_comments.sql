-- MAPB-1196: describe every table, view and column in the HMPPS Registers database.
--
-- These comments feed the published data dictionary: the SchemaSpy report and data-dictionary.csv at
-- https://ministryofjustice.github.io/prison-register/schema-spy-report/ (see .github/workflows/schema-spy.yml).
-- Keeping them in the database means the report, the CSV and anything else that reads the catalogue, such as
-- HMPPS Data Hub, share one source. SchemaCommentsTest fails the build if a new table, view or column is
-- added without one.
--
-- Every column comment ends with a sensitivity classification:
--
--   [Sensitivity: NONE]                - not personal data: codes, names and addresses of organisations,
--                                        flags and timestamps
--   [Sensitivity: PERSONAL]            - personal data about a prisoner
--   [Sensitivity: STAFF]               - personal data about a named member of staff, here a named contact
--                                        at an agency or a named person's work email address
--   [Sensitivity: SPECIAL-CATEGORY]    - UK GDPR Article 9 data, or criminal offence data under Article 10
--   [Sensitivity: OFFICIAL-SENSITIVE]  - not personal data, but damaging if disclosed
--
-- The register holds no prisoner data, so nothing is PERSONAL or SPECIAL-CATEGORY. The STAFF tags were set
-- on what the columns actually hold, counted (not read) in preprod on 26 November 2026: 81 of the 84 filled
-- probation office contacts and all 5 approved premises contacts look like a person's name, and 6 of 215
-- email addresses look like a named person's work address rather than a team mailbox.
--
-- Phone numbers, addresses and websites belong to prisons and other organisations and are published, so they
-- are NONE.
--
-- Every table and column was checked to exist in dev, preprod and prod before this was written. A comment on
-- a table missing from one environment would stop Flyway there, and the application would not start
-- (MAPA-350).

COMMENT ON TABLE address IS 'Postal addresses of prisons, with Welsh versions for prisons in Wales. Addresses of other agencies are in agency_address.';
COMMENT ON COLUMN address.id IS 'Surrogate key. [Sensitivity: NONE]';
COMMENT ON COLUMN address.prison_id IS 'The prison at this address. References prison.prison_id. [Sensitivity: NONE]';
COMMENT ON COLUMN address.address_line1 IS 'First line of the address. [Sensitivity: NONE]';
COMMENT ON COLUMN address.address_line2 IS 'Second line of the address. [Sensitivity: NONE]';
COMMENT ON COLUMN address.town IS 'Town or city. [Sensitivity: NONE]';
COMMENT ON COLUMN address.county IS 'County. [Sensitivity: NONE]';
COMMENT ON COLUMN address.postcode IS 'Postcode. [Sensitivity: NONE]';
COMMENT ON COLUMN address.country IS 'Country, for example England or Wales. [Sensitivity: NONE]';
COMMENT ON COLUMN address.address_line1_in_welsh IS 'First line of the address in Welsh. [Sensitivity: NONE]';
COMMENT ON COLUMN address.address_line2_in_welsh IS 'Second line of the address in Welsh. [Sensitivity: NONE]';
COMMENT ON COLUMN address.town_in_welsh IS 'Town or city in Welsh. [Sensitivity: NONE]';
COMMENT ON COLUMN address.county_in_welsh IS 'County in Welsh. [Sensitivity: NONE]';
COMMENT ON COLUMN address.country_in_welsh IS 'Country in Welsh. [Sensitivity: NONE]';

COMMENT ON TABLE agency IS 'Agencies of every type that does not have its own table, told apart by agency_type: airports, voluntary hostels, youth offending teams, secure children''s homes, immigration detention centres and so on. Migrated from NOMIS AGENCY_LOCATIONS.';
COMMENT ON COLUMN agency.agency_id IS 'Id of the agency: the NOMIS agency location id (AGENCY_LOCATIONS.AGY_LOC_ID), up to six characters and unique across every agency type in the register. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.name IS 'Short name of the agency, from NOMIS AGENCY_LOCATIONS.DESCRIPTION. Returned as description by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.description IS 'Full name of the agency, from NOMIS AGENCY_LOCATIONS.LONG_DESCRIPTION. Returned as longDescription by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.contact IS 'Name of a contact person at the agency, from NOMIS. Usually a named member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN agency.active IS 'Whether the agency is in use. Inactive ones are kept so that historical records still resolve. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.accessible_access IS 'Whether the building has accessible access: NONE, ACCESSIBLE, BY_ARRANGEMENT_ONLY or WHEELCHAIR_ACCESS. From the NOMIS disability access code. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.agency_type IS 'Type of agency, as the register''s type name: AIRPORT, PROBATION_CRC, FOREIGN_NATIONAL_PRISON, VOLUNTARY_HOSTEL, IMMIGRATION_DETENTION_CENTRE, OUTSIDE, PECS, PSYCHIATRIC_CARE, CHILDREN_SECURE_HOME, SECURE_TRAINING_CENTRE or YOT. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.inactive_date IS 'Date the agency was made inactive, if it has been. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.area IS 'NOMIS area (AGENCY_LOCATIONS.AREA_CODE). References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.geographical_area IS 'Geographical area (NOMIS AGENCY_LOCATIONS.GEOGRAPHIC_REGION_CODE), for example a county. References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.region IS 'Region (NOMIS AGENCY_LOCATIONS.NOMS_REGION_CODE). References region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.cjit_code IS 'Criminal Justice Integrated Team (CJIT) code from NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.payroll_region IS 'Prisoner payroll region from NOMIS. References payroll_region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN agency.local_authority IS 'Local authority from NOMIS. References local_authority.code. [Sensitivity: NONE]';

COMMENT ON TABLE agency_address IS 'Postal addresses of agencies other than prisons. Linked to their agency through the <agency type>_to_agency_address tables.';
COMMENT ON COLUMN agency_address.id IS 'Surrogate key. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_address.address_line1 IS 'First line of the address. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_address.address_line2 IS 'Second line of the address. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_address.town IS 'Town or city. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_address.county IS 'County. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_address.postcode IS 'Postcode. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_address.country IS 'Country. [Sensitivity: NONE]';

COMMENT ON TABLE agency_to_agency_address IS 'Links each agency to its addresses: one row per agency and address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN agency_to_agency_address.agency_id IS 'The agency. References agency.agency_id. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_to_agency_address.agency_address_id IS 'The address. References agency_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE agency_to_email_address IS 'Links each agency to its email addresses: one row per agency and email address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN agency_to_email_address.agency_id IS 'The agency. References agency.agency_id. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_to_email_address.email_address_id IS 'The email address. References email_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE agency_to_phone IS 'Links each agency to its phone numbers: one row per agency and phone number. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN agency_to_phone.agency_id IS 'The agency. References agency.agency_id. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_to_phone.phone_id IS 'The phone number. References phone_number.id. [Sensitivity: NONE]';

COMMENT ON TABLE agency_type IS 'NOMIS agency type codes and their descriptions, for example APPR Approved Premises. Reference only: the API does not read this table, and agency.agency_type holds the register''s type names rather than these codes.';
COMMENT ON COLUMN agency_type.code IS 'Code, for example APPR. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_type.description IS 'Description, for example Approved Premises. [Sensitivity: NONE]';
COMMENT ON COLUMN agency_type.active IS 'Whether the code can still be used. [Sensitivity: NONE]';

COMMENT ON VIEW all_agencies IS 'View of every agency id in the register, of any type, with its name and type. The API uses it to check that a new agency id is not already taken.';
COMMENT ON COLUMN all_agencies.agency_id IS 'Id of the agency. [Sensitivity: NONE]';
COMMENT ON COLUMN all_agencies.name IS 'Short name of the agency. [Sensitivity: NONE]';
COMMENT ON COLUMN all_agencies.legacy_type IS 'Type of agency, as the register''s type name: PRISON, COURT, HOSPITAL, SECURE_HOSPITAL, PROBATION_OFFICE, POLICE_CUSTODY_SUITE, APPROVED_PREMISES or one of the agency.agency_type values. Despite the column name, these are not NOMIS codes. [Sensitivity: NONE]';

COMMENT ON TABLE approved_premises IS 'Approved premises: one row each. Migrated from NOMIS AGENCY_LOCATIONS (agency type APPR).';
COMMENT ON COLUMN approved_premises.approved_premises_id IS 'Id of the approved premises: the NOMIS agency location id (AGENCY_LOCATIONS.AGY_LOC_ID), up to six characters and unique across every agency type in the register. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.name IS 'Short name of the approved premises, from NOMIS AGENCY_LOCATIONS.DESCRIPTION. Returned as description by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.description IS 'Full name of the approved premises, from NOMIS AGENCY_LOCATIONS.LONG_DESCRIPTION. Returned as longDescription by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.contact IS 'Name of a contact person at the approved premises, from NOMIS. Usually a named member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN approved_premises.active IS 'Whether the approved premises is in use. Inactive ones are kept so that historical records still resolve. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.accessible_access IS 'Whether the building has accessible access: NONE, ACCESSIBLE, BY_ARRANGEMENT_ONLY or WHEELCHAIR_ACCESS. From the NOMIS disability access code. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.inactive_date IS 'Date the approved premises was made inactive, if it has been. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.area IS 'NOMIS area (AGENCY_LOCATIONS.AREA_CODE). References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.geographical_area IS 'Geographical area (NOMIS AGENCY_LOCATIONS.GEOGRAPHIC_REGION_CODE), for example a county. References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.region IS 'Region (NOMIS AGENCY_LOCATIONS.NOMS_REGION_CODE). References region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.cjit_code IS 'Criminal Justice Integrated Team (CJIT) code from NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.local_authority IS 'Local authority from NOMIS. References local_authority.code. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises.payroll_region IS 'Prisoner payroll region from NOMIS. References payroll_region.code. [Sensitivity: NONE]';

COMMENT ON TABLE approved_premises_to_agency_address IS 'Links each approved premises to its addresses: one row per approved premises and address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN approved_premises_to_agency_address.approved_premises_id IS 'The approved premises. References approved_premises.approved_premises_id. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises_to_agency_address.agency_address_id IS 'The address. References agency_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE approved_premises_to_email_address IS 'Links each approved premises to its email addresses: one row per approved premises and email address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN approved_premises_to_email_address.approved_premises_id IS 'The approved premises. References approved_premises.approved_premises_id. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises_to_email_address.email_address_id IS 'The email address. References email_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE approved_premises_to_phone IS 'Links each approved premises to its phone numbers: one row per approved premises and phone number. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN approved_premises_to_phone.approved_premises_id IS 'The approved premises. References approved_premises.approved_premises_id. [Sensitivity: NONE]';
COMMENT ON COLUMN approved_premises_to_phone.phone_id IS 'The phone number. References phone_number.id. [Sensitivity: NONE]';

COMMENT ON TABLE area IS 'Areas from NOMIS. Holds both prison areas and geographical areas such as counties, told apart by prison_area. Each belongs to a region.';
COMMENT ON COLUMN area.code IS 'Code, for example YORK. [Sensitivity: NONE]';
COMMENT ON COLUMN area.parent IS 'The region the area belongs to. References region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN area.description IS 'Description, for example Yorkshire & Humberside. [Sensitivity: NONE]';
COMMENT ON COLUMN area.prison_area IS 'Whether this is a prison area (true) or a geographical area such as a county (false). [Sensitivity: NONE]';
COMMENT ON COLUMN area.active IS 'Whether the code can still be used. [Sensitivity: NONE]';

COMMENT ON TABLE contact_details IS 'Prison departments that have contact details: one row per prison and department. The phone number, email address and website hang off it through the contact_details_to_* tables.';
COMMENT ON COLUMN contact_details.id IS 'Surrogate key. [Sensitivity: NONE]';
COMMENT ON COLUMN contact_details.prison_id IS 'The prison. References prison.prison_id. [Sensitivity: NONE]';
COMMENT ON COLUMN contact_details.department_type IS 'The department: PRISON, SOCIAL_VISIT, VIDEOLINK_CONFERENCING_CENTRE, OFFENDER_MANAGEMENT_UNIT or OFFICIAL_VISIT. [Sensitivity: NONE]';
COMMENT ON COLUMN contact_details.create_timestamp IS 'When the row was created. Set by the application. [Sensitivity: NONE]';
COMMENT ON COLUMN contact_details.modify_timestamp IS 'When the row itself was last changed. Set by the application. A change to the linked phone number, email address or website alone may not move it. [Sensitivity: NONE]';

COMMENT ON TABLE contact_details_to_email_address IS 'Links a prison department to its email address. A department has at most one.';
COMMENT ON COLUMN contact_details_to_email_address.contact_details_id IS 'The prison department. References contact_details.id. [Sensitivity: NONE]';
COMMENT ON COLUMN contact_details_to_email_address.email_address_id IS 'The email address. References email_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE contact_details_to_phone_number IS 'Links a prison department to its phone number. A department has at most one.';
COMMENT ON COLUMN contact_details_to_phone_number.contact_details_id IS 'The prison department. References contact_details.id. [Sensitivity: NONE]';
COMMENT ON COLUMN contact_details_to_phone_number.phone_number_id IS 'The phone number. References phone_number.id. [Sensitivity: NONE]';

COMMENT ON TABLE contact_details_to_web_address IS 'Links a prison department to its website. A department has at most one.';
COMMENT ON COLUMN contact_details_to_web_address.contact_details_id IS 'The prison department. References contact_details.id. [Sensitivity: NONE]';
COMMENT ON COLUMN contact_details_to_web_address.web_address_id IS 'The website. References web_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE county_suggestions IS 'County names offered as suggestions when an address is typed in the registers front end.';
COMMENT ON COLUMN county_suggestions.code IS 'Code, for example ANGUS. [Sensitivity: NONE]';
COMMENT ON COLUMN county_suggestions.description IS 'Description, for example Angus. [Sensitivity: NONE]';

COMMENT ON TABLE court IS 'Courts: one row each. Migrated from NOMIS AGENCY_LOCATIONS (agency type CRT).';
COMMENT ON COLUMN court.court_id IS 'Id of the court: the NOMIS agency location id (AGENCY_LOCATIONS.AGY_LOC_ID), up to six characters and unique across every agency type in the register. [Sensitivity: NONE]';
COMMENT ON COLUMN court.name IS 'Short name of the court, from NOMIS AGENCY_LOCATIONS.DESCRIPTION. Returned as description by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN court.description IS 'Full name of the court, from NOMIS AGENCY_LOCATIONS.LONG_DESCRIPTION. Returned as longDescription by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN court.active IS 'Whether the court is in use. Inactive ones are kept so that historical records still resolve. [Sensitivity: NONE]';
COMMENT ON COLUMN court.inactive_date IS 'Date the court was made inactive, if it has been. [Sensitivity: NONE]';
COMMENT ON COLUMN court.court_type IS 'Type of court. References court_type.code (enforced by the application, not by a database foreign key). [Sensitivity: NONE]';
COMMENT ON COLUMN court.area IS 'NOMIS area (AGENCY_LOCATIONS.AREA_CODE). References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN court.region IS 'Region (NOMIS AGENCY_LOCATIONS.NOMS_REGION_CODE). References region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN court.cjit_code IS 'Criminal Justice Integrated Team (CJIT) code from NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN court.geographical_area IS 'Geographical area (NOMIS AGENCY_LOCATIONS.GEOGRAPHIC_REGION_CODE), for example a county. References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN court.local_authority IS 'Local authority from NOMIS. References local_authority.code. [Sensitivity: NONE]';
COMMENT ON COLUMN court.payroll_region IS 'Prisoner payroll region from NOMIS. References payroll_region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN court.accessible_access IS 'Whether the building has accessible access: NONE, ACCESSIBLE, BY_ARRANGEMENT_ONLY or WHEELCHAIR_ACCESS. From the NOMIS disability access code. [Sensitivity: NONE]';

COMMENT ON TABLE court_to_agency_address IS 'Links each court to its addresses: one row per court and address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN court_to_agency_address.court_id IS 'The court. References court.court_id. [Sensitivity: NONE]';
COMMENT ON COLUMN court_to_agency_address.agency_address_id IS 'The address. References agency_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE court_to_email_address IS 'Links each court to its email addresses: one row per court and email address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN court_to_email_address.court_id IS 'The court. References court.court_id. [Sensitivity: NONE]';
COMMENT ON COLUMN court_to_email_address.email_address_id IS 'The email address. References email_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE court_to_phone IS 'Links each court to its phone numbers: one row per court and phone number. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN court_to_phone.court_id IS 'The court. References court.court_id. [Sensitivity: NONE]';
COMMENT ON COLUMN court_to_phone.phone_id IS 'The phone number. References phone_number.id. [Sensitivity: NONE]';

COMMENT ON TABLE court_type IS 'Types of court.';
COMMENT ON COLUMN court_type.code IS 'Code, for example CC. [Sensitivity: NONE]';
COMMENT ON COLUMN court_type.description IS 'Description, for example Crown Court. [Sensitivity: NONE]';
COMMENT ON COLUMN court_type.active IS 'Whether the code can still be used. [Sensitivity: NONE]';

COMMENT ON TABLE email_address IS 'Email addresses of prison departments and other agencies. Since V1_65 every owner has its own rows, so the same address can appear more than once.';
COMMENT ON COLUMN email_address.id IS 'Surrogate key. [Sensitivity: NONE]';
COMMENT ON COLUMN email_address.value IS 'The email address. Mostly team mailboxes, but a few are a named member of staff''s work address. [Sensitivity: STAFF]';

COMMENT ON TABLE hospital IS 'Hospitals: one row each. Migrated from NOMIS AGENCY_LOCATIONS (agency type HOSPITAL or HSHOSP).';
COMMENT ON COLUMN hospital.hospital_id IS 'Id of the hospital: the NOMIS agency location id (AGENCY_LOCATIONS.AGY_LOC_ID), up to six characters and unique across every agency type in the register. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.name IS 'Short name of the hospital, from NOMIS AGENCY_LOCATIONS.DESCRIPTION. Returned as description by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.description IS 'Full name of the hospital, from NOMIS AGENCY_LOCATIONS.LONG_DESCRIPTION. Returned as longDescription by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.active IS 'Whether the hospital is in use. Inactive ones are kept so that historical records still resolve. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.high_security IS 'Whether this is a secure hospital (NOMIS type HSHOSP) rather than an ordinary one (HOSPITAL). [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.inactive_date IS 'Date the hospital was made inactive, if it has been. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.area IS 'NOMIS area (AGENCY_LOCATIONS.AREA_CODE). References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.geographical_area IS 'Geographical area (NOMIS AGENCY_LOCATIONS.GEOGRAPHIC_REGION_CODE), for example a county. References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.region IS 'Region (NOMIS AGENCY_LOCATIONS.NOMS_REGION_CODE). References region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.payroll_region IS 'Prisoner payroll region from NOMIS. References payroll_region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.cjit_code IS 'Criminal Justice Integrated Team (CJIT) code from NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital.local_authority IS 'Local authority from NOMIS. References local_authority.code. [Sensitivity: NONE]';

COMMENT ON TABLE hospital_to_agency_address IS 'Links each hospital to its addresses: one row per hospital and address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN hospital_to_agency_address.hospital_id IS 'The hospital. References hospital.hospital_id. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital_to_agency_address.agency_address_id IS 'The address. References agency_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE hospital_to_phone IS 'Links each hospital to its phone numbers: one row per hospital and phone number. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN hospital_to_phone.hospital_id IS 'The hospital. References hospital.hospital_id. [Sensitivity: NONE]';
COMMENT ON COLUMN hospital_to_phone.phone_id IS 'The phone number. References phone_number.id. [Sensitivity: NONE]';

COMMENT ON TABLE local_authority IS 'Local authorities from NOMIS.';
COMMENT ON COLUMN local_authority.code IS 'Code, for example 00AA. [Sensitivity: NONE]';
COMMENT ON COLUMN local_authority.description IS 'Description, for example City of London. [Sensitivity: NONE]';
COMMENT ON COLUMN local_authority.active IS 'Whether the code can still be used. [Sensitivity: NONE]';

COMMENT ON TABLE operator IS 'Who runs prisons: PSP for the public sector, or the name of a private provider.';
COMMENT ON COLUMN operator.id IS 'Surrogate key. [Sensitivity: NONE]';
COMMENT ON COLUMN operator.name IS 'Name of the operator, for example PSP or the provider''s name. [Sensitivity: NONE]';

COMMENT ON TABLE payroll_region IS 'Prisoner payroll regions from NOMIS.';
COMMENT ON COLUMN payroll_region.code IS 'Code, for example HS. [Sensitivity: NONE]';
COMMENT ON COLUMN payroll_region.description IS 'Description, for example High Security. [Sensitivity: NONE]';
COMMENT ON COLUMN payroll_region.active IS 'Whether the code can still be used. [Sensitivity: NONE]';

COMMENT ON TABLE phone_number IS 'Phone numbers of prison departments and other agencies. Since V1_65 every owner has its own rows, so the same number can appear more than once.';
COMMENT ON COLUMN phone_number.id IS 'Surrogate key. [Sensitivity: NONE]';
COMMENT ON COLUMN phone_number.value IS 'The phone number as entered. Phone numbers have no type, so business and fax numbers cannot be told apart. [Sensitivity: NONE]';

COMMENT ON TABLE police_custody_suite IS 'Police custody suites: one row each. Migrated from NOMIS AGENCY_LOCATIONS (agency type POLICE or POLSTN).';
COMMENT ON COLUMN police_custody_suite.police_custody_suite_id IS 'Id of the police custody suite: the NOMIS agency location id (AGENCY_LOCATIONS.AGY_LOC_ID), up to six characters and unique across every agency type in the register. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.name IS 'Short name of the police custody suite, from NOMIS AGENCY_LOCATIONS.DESCRIPTION. Returned as description by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.description IS 'Full name of the police custody suite, from NOMIS AGENCY_LOCATIONS.LONG_DESCRIPTION. Returned as longDescription by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.active IS 'Whether the police custody suite is in use. Inactive ones are kept so that historical records still resolve. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.inactive_date IS 'Date the police custody suite was made inactive, if it has been. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.area IS 'NOMIS area (AGENCY_LOCATIONS.AREA_CODE). References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.geographical_area IS 'Geographical area (NOMIS AGENCY_LOCATIONS.GEOGRAPHIC_REGION_CODE), for example a county. References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.region IS 'Region (NOMIS AGENCY_LOCATIONS.NOMS_REGION_CODE). References region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.cjit_code IS 'Criminal Justice Integrated Team (CJIT) code from NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.local_authority IS 'Local authority from NOMIS. References local_authority.code. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite.payroll_region IS 'Prisoner payroll region from NOMIS. References payroll_region.code. [Sensitivity: NONE]';

COMMENT ON TABLE police_custody_suite_to_agency_address IS 'Links each police custody suite to its addresses: one row per police custody suite and address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN police_custody_suite_to_agency_address.police_custody_suite_id IS 'The police custody suite. References police_custody_suite.police_custody_suite_id. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite_to_agency_address.agency_address_id IS 'The address. References agency_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE police_custody_suite_to_email_address IS 'Links each police custody suite to its email addresses: one row per police custody suite and email address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN police_custody_suite_to_email_address.police_custody_suite_id IS 'The police custody suite. References police_custody_suite.police_custody_suite_id. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite_to_email_address.email_address_id IS 'The email address. References email_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE police_custody_suite_to_phone IS 'Links each police custody suite to its phone numbers: one row per police custody suite and phone number. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN police_custody_suite_to_phone.police_custody_suite_id IS 'The police custody suite. References police_custody_suite.police_custody_suite_id. [Sensitivity: NONE]';
COMMENT ON COLUMN police_custody_suite_to_phone.phone_id IS 'The phone number. References phone_number.id. [Sensitivity: NONE]';

COMMENT ON TABLE prison IS 'Prisons, young offender institutions and similar establishments: one row per prison. New prisons are created in NOMIS and arrive through the sync from NOMIS.';
COMMENT ON COLUMN prison.prison_id IS 'Id of the prison: the NOMIS agency location id, for example MDI. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.name IS 'Name of the prison as DPS shows it, for example Moorland (HMP & YOI). Standardised in V1_22 and deliberately different from the NOMIS description. Printed on Send Legal Mail envelopes. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.active IS 'Whether the prison is open. Closed prisons are kept so that historical records still resolve. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.description IS 'The NOMIS description of the prison (AGENCY_LOCATIONS.DESCRIPTION), for example HMP & YOI MOORLAND. Kept up to date by the sync from NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.male IS 'Whether the prison holds men. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.female IS 'Whether the prison holds women. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.inactive_date IS 'Date the prison was closed, if it has been. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.contracted IS 'Whether a private provider runs the prison under contract, rather than the public sector (PSP). [Sensitivity: NONE]';
COMMENT ON COLUMN prison.lthse IS 'Whether the prison is part of the long-term high security estate. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.gp_practice_code IS 'NHS GP practice code for the prison''s healthcare. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.prison_name_in_welsh IS 'Name of the prison in Welsh, for prisons in Wales, for example Carchar Brynbuga. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.area IS 'NOMIS area of the prison (AGENCY_LOCATIONS.AREA_CODE). References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.geographical_area IS 'Geographical area of the prison (NOMIS AGENCY_LOCATIONS.GEOGRAPHIC_REGION_CODE), for example a county. References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.region IS 'Region of the prison (NOMIS AGENCY_LOCATIONS.NOMS_REGION_CODE), for example YOHUM. References region.code. Use this for regional reporting in place of the NOMIS AREAS table. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.cjit_code IS 'Criminal Justice Integrated Team (CJIT) code from NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.payroll_region IS 'Prisoner payroll region from NOMIS. References payroll_region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN prison.local_authority IS 'Local authority the prison is in, from NOMIS. References local_authority.code. [Sensitivity: NONE]';

COMMENT ON TABLE prison_category IS 'Security categories each prison holds: one row per prison and category.';
COMMENT ON COLUMN prison_category.category IS 'The category: A, B, C, D, OPEN or CLOSED. [Sensitivity: NONE]';
COMMENT ON COLUMN prison_category.prison_id IS 'The prison. References prison.prison_id. [Sensitivity: NONE]';

COMMENT ON TABLE prison_operator IS 'Links each prison to its operator. A prison normally has one row, and the API treats the first one as the current operator.';
COMMENT ON COLUMN prison_operator.prison_id IS 'The prison. References prison.prison_id. [Sensitivity: NONE]';
COMMENT ON COLUMN prison_operator.operator_id IS 'The operator. References operator.id. [Sensitivity: NONE]';

COMMENT ON TABLE prison_type IS 'Types of each prison: one row per prison and type. A prison can have more than one, for example HMP and YOI.';
COMMENT ON COLUMN prison_type.id IS 'Surrogate key. [Sensitivity: NONE]';
COMMENT ON COLUMN prison_type.prison_id IS 'The prison. References prison.prison_id. [Sensitivity: NONE]';
COMMENT ON COLUMN prison_type.type IS 'The type. References type.id. [Sensitivity: NONE]';

COMMENT ON TABLE probation_office IS 'Probation offices: one row each. Migrated from NOMIS AGENCY_LOCATIONS (agency type COMM).';
COMMENT ON COLUMN probation_office.probation_office_id IS 'Id of the probation office: the NOMIS agency location id (AGENCY_LOCATIONS.AGY_LOC_ID), up to six characters and unique across every agency type in the register. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.name IS 'Short name of the probation office, from NOMIS AGENCY_LOCATIONS.DESCRIPTION. Returned as description by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.description IS 'Full name of the probation office, from NOMIS AGENCY_LOCATIONS.LONG_DESCRIPTION. Returned as longDescription by /api/agencies. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.active IS 'Whether the probation office is in use. Inactive ones are kept so that historical records still resolve. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.accessible_access IS 'Whether the building has accessible access: NONE, ACCESSIBLE, BY_ARRANGEMENT_ONLY or WHEELCHAIR_ACCESS. From the NOMIS disability access code. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.inactive_date IS 'Date the probation office was made inactive, if it has been. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.area IS 'NOMIS area (AGENCY_LOCATIONS.AREA_CODE). References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.geographical_area IS 'Geographical area (NOMIS AGENCY_LOCATIONS.GEOGRAPHIC_REGION_CODE), for example a county. References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.region IS 'Region (NOMIS AGENCY_LOCATIONS.NOMS_REGION_CODE). References region.code. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.cjit_code IS 'Criminal Justice Integrated Team (CJIT) code from NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.local_authority IS 'Local authority from NOMIS. References local_authority.code. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.subarea IS 'Probation sub-area from NOMIS. References subarea.code. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office.contact IS 'Name of a contact person at the probation office, from NOMIS. Usually a named member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN probation_office.payroll_region IS 'Prisoner payroll region from NOMIS. References payroll_region.code. [Sensitivity: NONE]';

COMMENT ON TABLE probation_office_to_agency_address IS 'Links each probation office to its addresses: one row per probation office and address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN probation_office_to_agency_address.probation_office_id IS 'The probation office. References probation_office.probation_office_id. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office_to_agency_address.agency_address_id IS 'The address. References agency_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE probation_office_to_email_address IS 'Links each probation office to its email addresses: one row per probation office and email address. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN probation_office_to_email_address.probation_office_id IS 'The probation office. References probation_office.probation_office_id. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office_to_email_address.email_address_id IS 'The email address. References email_address.id. [Sensitivity: NONE]';

COMMENT ON TABLE probation_office_to_phone IS 'Links each probation office to its phone numbers: one row per probation office and phone number. The primary key stops the same link being stored twice.';
COMMENT ON COLUMN probation_office_to_phone.probation_office_id IS 'The probation office. References probation_office.probation_office_id. [Sensitivity: NONE]';
COMMENT ON COLUMN probation_office_to_phone.phone_id IS 'The phone number. References phone_number.id. [Sensitivity: NONE]';

COMMENT ON TABLE region IS 'Regions from NOMIS, used to group prisons and other agencies.';
COMMENT ON COLUMN region.code IS 'Code, for example YOHUM. [Sensitivity: NONE]';
COMMENT ON COLUMN region.description IS 'Description, for example Yorkshire & Humberside. [Sensitivity: NONE]';
COMMENT ON COLUMN region.active IS 'Whether the code can still be used. [Sensitivity: NONE]';

COMMENT ON TABLE subarea IS 'Probation sub-areas from NOMIS, each within an area. Used by probation offices.';
COMMENT ON COLUMN subarea.code IS 'Code, for example CITY. [Sensitivity: NONE]';
COMMENT ON COLUMN subarea.parent IS 'The area the sub-area belongs to. References area.code. [Sensitivity: NONE]';
COMMENT ON COLUMN subarea.description IS 'Description, for example City. [Sensitivity: NONE]';
COMMENT ON COLUMN subarea.active IS 'Whether the code can still be used. [Sensitivity: NONE]';

COMMENT ON TABLE town_suggestions IS 'Town names offered as suggestions when an address is typed in the registers front end.';
COMMENT ON COLUMN town_suggestions.code IS 'Code, for example 10001. [Sensitivity: NONE]';
COMMENT ON COLUMN town_suggestions.description IS 'Description, for example Wadhurst. [Sensitivity: NONE]';

COMMENT ON TABLE type IS 'Prison types and their descriptions: HMP, YOI, IRC, STC and YCS.';
COMMENT ON COLUMN type.id IS 'Type code, for example HMP. [Sensitivity: NONE]';
COMMENT ON COLUMN type.description IS 'Description, for example His Majesty''s Prison. [Sensitivity: NONE]';

COMMENT ON TABLE web_address IS 'Website addresses of prison departments.';
COMMENT ON COLUMN web_address.id IS 'Surrogate key. [Sensitivity: NONE]';
COMMENT ON COLUMN web_address.value IS 'The website address. [Sensitivity: NONE]';

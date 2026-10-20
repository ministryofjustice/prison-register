create or replace view all_agencies as
select prison_id as agency_id, name, 'PRISON' as legacy_type
from prison
union all
select court_id, name, 'COURT'
from court
union all
select hospital_id, name, case when high_security then 'SECURE_HOSPITAL' else 'HOSPITAL' end
from hospital
union all
select probation_office_id, name, 'PROBATION_OFFICE'
from probation_office
union all
select police_custody_suite_id, name, 'POLICE_CUSTODY_SUITE'
from police_custody_suite
union all
select approved_premises_id, name, 'APPROVED_PREMISES'
from approved_premises
union all
select agency_id, name, agency_type
from agency;

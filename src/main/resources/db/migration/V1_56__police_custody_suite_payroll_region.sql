alter table police_custody_suite add column payroll_region varchar(12);
alter table police_custody_suite add constraint police_custody_suite_payroll_region_fk foreign key (payroll_region) references payroll_region (code);

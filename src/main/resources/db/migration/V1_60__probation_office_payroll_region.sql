alter table probation_office
    add column payroll_region varchar(12),
    add constraint probation_office_payroll_region_fk foreign key (payroll_region) references payroll_region (code);

alter table court
    add column payroll_region varchar(12),
    add constraint court_payroll_region_fk foreign key (payroll_region) references payroll_region (code);

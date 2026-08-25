alter table approved_premise
    add column payroll_region varchar(12),
    add constraint approved_premise_payroll_region_fk foreign key (payroll_region) references payroll_region (code);

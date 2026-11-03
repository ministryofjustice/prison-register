alter table prison
    add column area varchar(12),
    add column geographical_area varchar(12),
    add column region varchar(12),
    add column cjit_code varchar(12),
    add column payroll_region varchar(12),
    add column local_authority varchar(12),
    add constraint prison_area_fk foreign key (area) references area (code),
    add constraint prison_geographical_area_fk foreign key (geographical_area) references area (code),
    add constraint prison_region_fk foreign key (region) references region (code),
    add constraint prison_payroll_region_fk foreign key (payroll_region) references payroll_region (code),
    add constraint prison_local_authority_fk foreign key (local_authority) references local_authority (code);

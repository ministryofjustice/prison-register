create table hospital
(
    hospital_id       varchar(6)  not null primary key,
    name              varchar(40) not null,
    description       varchar(3000),
    active            boolean     not null,
    high_security     boolean     not null,
    inactive_date     date,
    area              varchar(12),
    geographical_area varchar(12),
    region            varchar(12),
    payroll_region    varchar(12),
    cjit_code         varchar(12),
    constraint hospital_area_fk foreign key (area) references area (code),
    constraint hospital_geographical_area_fk foreign key (geographical_area) references area (code),
    constraint hospital_region_fk foreign key (region) references region (code),
    constraint hospital_payroll_region_fk foreign key (payroll_region) references payroll_region (code)
);


create table hospital_to_agency_address
(
    hospital_id       varchar(6) not null,
    agency_address_id integer    not null,
    constraint hospital_to_agency_address_to_hospital_fk foreign key (hospital_id) references hospital (hospital_id),
    constraint hospital_to_agency_address_to_agency_address_fk foreign key (agency_address_id) references agency_address (id)
);

create index hospital_to_agency_address_idx on hospital_to_agency_address (hospital_id, agency_address_id);


create table hospital_to_phone
(
    hospital_id varchar(6) not null,
    phone_id    integer    not null,
    constraint hospital_to_phone_to_hospital_fk foreign key (hospital_id) references hospital (hospital_id),
    constraint hospital_to_phone_to_phone_fk foreign key (phone_id) references phone_number (id)
);


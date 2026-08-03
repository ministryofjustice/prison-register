create table police_custody_suite
(
    police_custody_suite_id varchar(6)  not null primary key,
    name                varchar(40) not null,
    description         varchar(3000),
    active              boolean     not null,
    inactive_date       date,
    area                varchar(12),
    geographical_area   varchar(12),
    region              varchar(12),
    cjit_code           varchar(12),
    constraint police_custody_suite_area_fk foreign key (area) references area (code),
    constraint police_custody_suite_geographical_area_fk foreign key (geographical_area) references area (code),
    constraint police_custody_suite_region_fk foreign key (region) references region (code)
);


create table police_custody_suite_to_agency_address
(
    police_custody_suite_id varchar(6) not null,
    agency_address_id   integer    not null,
    constraint police_custody_suite_to_agency_address_to_pcs_fk foreign key (police_custody_suite_id) references police_custody_suite (police_custody_suite_id),
    constraint police_custody_suite_to_agency_address_to_agency_address_fk foreign key (agency_address_id) references agency_address (id)
);

create index police_custody_suite_to_agency_address_idx on police_custody_suite_to_agency_address (police_custody_suite_id, agency_address_id);


create table police_custody_suite_to_phone
(
    police_custody_suite_id varchar(6) not null,
    phone_id            integer    not null,
    constraint police_custody_suite_to_phone_to_pcs_fk foreign key (police_custody_suite_id) references police_custody_suite (police_custody_suite_id),
    constraint police_custody_suite_to_phone_to_phone_fk foreign key (phone_id) references phone_number (id)
);

create table police_custody_suite_to_email_address
(
    police_custody_suite_id          varchar(6) not null,
    email_address_id integer    not null,
    constraint police_custody_suite_to_email_address_to_pcs_fk foreign key (police_custody_suite_id) references police_custody_suite (police_custody_suite_id),
    constraint police_custody_suite_to_email_address_to_email_address_fk foreign key (email_address_id) references email_address (id)
);


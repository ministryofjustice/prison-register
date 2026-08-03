create table probation_office
(
    probation_office_id varchar(6)  not null primary key,
    name                varchar(40) not null,
    description         varchar(3000),
    active              boolean     not null,
    accessible_access   varchar(20),
    inactive_date       date,
    area                varchar(12),
    geographical_area   varchar(12),
    region              varchar(12),
    cjit_code           varchar(12),
    constraint probation_office_area_fk foreign key (area) references area (code),
    constraint probation_office_geographical_area_fk foreign key (geographical_area) references area (code),
    constraint probation_office_region_fk foreign key (region) references region (code)
);


create table probation_office_to_agency_address
(
    probation_office_id varchar(6) not null,
    agency_address_id   integer    not null,
    constraint probation_office_to_agency_address_to_probation_office_fk foreign key (probation_office_id) references probation_office (probation_office_id),
    constraint probation_office_to_agency_address_to_agency_address_fk foreign key (agency_address_id) references agency_address (id)
);

create index probation_office_to_agency_address_idx on probation_office_to_agency_address (probation_office_id, agency_address_id);


create table probation_office_to_phone
(
    probation_office_id varchar(6) not null,
    phone_id            integer    not null,
    constraint probation_office_to_phone_to_probation_office_fk foreign key (probation_office_id) references probation_office (probation_office_id),
    constraint probation_office_to_phone_to_phone_fk foreign key (phone_id) references phone_number (id)
);

create table probation_office_to_email_address
(
    probation_office_id          varchar(6) not null,
    email_address_id integer    not null,
    constraint probation_office_to_email_address_to_probation_office_fk foreign key (probation_office_id) references probation_office (probation_office_id),
    constraint probation_office_to_email_address_to_email_address_fk foreign key (email_address_id) references email_address (id)
);


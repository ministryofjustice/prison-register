create table agency
(
    agency_id         varchar(6)  not null primary key,
    name              varchar(40) not null,
    description       varchar(3000),
    contact           varchar(40),
    active            boolean     not null,
    accessible_access varchar(20),
    agency_type       varchar(30),
    inactive_date     date,
    area              varchar(12),
    geographical_area varchar(12),
    region            varchar(12),
    cjit_code         varchar(12),
    payroll_region    varchar(12),
    constraint agency_area_fk foreign key (area) references area (code),
    constraint agency_geographical_area_fk foreign key (geographical_area) references area (code),
    constraint agency_region_fk foreign key (region) references region (code),
    constraint agency_payroll_region_fk foreign key (payroll_region) references payroll_region (code)
);


create table agency_to_agency_address
(
    agency_id         varchar(6) not null,
    agency_address_id integer    not null,
    constraint agency_to_agency_address_to_agency_fk foreign key (agency_id) references agency (agency_id),
    constraint agency_to_agency_address_to_agency_address_fk foreign key (agency_address_id) references agency_address (id)
);

create index agency_to_agency_address_idx on agency_to_agency_address (agency_id, agency_address_id);


create table agency_to_phone
(
    agency_id varchar(6) not null,
    phone_id  integer    not null,
    constraint agency_to_phone_to_agency_fk foreign key (agency_id) references agency (agency_id),
    constraint agency_to_phone_to_phone_fk foreign key (phone_id) references phone_number (id)
);

create index agency_to_phone_idx on agency_to_phone (agency_id, phone_id);

create table agency_to_email_address
(
    agency_id        varchar(6) not null,
    email_address_id integer    not null,
    constraint agency_to_email_address_to_agency_fk foreign key (agency_id) references agency (agency_id),
    constraint agency_to_email_address_to_email_address_fk foreign key (email_address_id) references email_address (id)
);

create index agency_to_email_address_idx on agency_to_email_address (agency_id, email_address_id);


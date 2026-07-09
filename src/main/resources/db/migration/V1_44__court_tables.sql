create table court
(
    court_id      varchar(6)  not null primary key,
    name          varchar(40) not null,
    description   varchar(3000),
    active        boolean     not null,
    inactive_date date,
    court_type    varchar(12) not null,
    area          varchar(12),
    region        varchar(12),
    cjit_code     varchar(12),
    constraint court_area_fk foreign key (area) references area (code),
    constraint court_region_fk foreign key (region) references region (code)
);

create table agency_address
(
    id              serial primary key,
    address_line1 varchar(350),
    address_line2 varchar(80),
    town          varchar(80),
    county        varchar(80),
    postcode      varchar(8),
    country       varchar(16)
);

create table court_to_agency_address
(
    court_id          varchar(6) not null,
    agency_address_id integer    not null,
    constraint court_to_agency_address_to_court_fk foreign key (court_id) references court (court_id),
    constraint court_to_agency_address_to_agency_address_fk foreign key (agency_address_id) references agency_address (id)
);

create index court_to_agency_address_idx on court_to_agency_address (court_id, agency_address_id);


create table court_to_phone
(
    court_id          varchar(6) not null,
    phone_id integer    not null,
    constraint court_to_phone_to_court_fk foreign key (court_id) references court (court_id),
    constraint court_to_phone_to_phone_fk foreign key (phone_id) references phone_number (id)
);

create table court_to_email_address
(
    court_id          varchar(6) not null,
    email_address_id integer    not null,
    constraint court_to_email_address_to_court_fk foreign key (court_id) references court (court_id),
    constraint court_to_email_address_to_email_address_fk foreign key (email_address_id) references email_address (id)
);

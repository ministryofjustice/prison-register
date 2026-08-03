create table approved_premise
(
    approved_premise_id varchar(6)  not null primary key,
    name                varchar(40) not null,
    description         varchar(3000),
    contact             varchar(40),
    active              boolean     not null,
    accessible_access   varchar(20),
    inactive_date       date,
    area                varchar(12),
    geographical_area   varchar(12),
    region              varchar(12),
    cjit_code           varchar(12),
    constraint approved_premise_area_fk foreign key (area) references area (code),
    constraint approved_premise_geographical_area_fk foreign key (geographical_area) references area (code),
    constraint approved_premise_region_fk foreign key (region) references region (code)
);


create table approved_premise_to_agency_address
(
    approved_premise_id varchar(6) not null,
    agency_address_id   integer    not null,
    constraint approved_premise_to_agency_address_to_approved_premise_fk foreign key (approved_premise_id) references approved_premise (approved_premise_id),
    constraint approved_premise_to_agency_address_to_agency_address_fk foreign key (agency_address_id) references agency_address (id)
);

create index approved_premise_to_agency_address_idx on approved_premise_to_agency_address (approved_premise_id, agency_address_id);


create table approved_premise_to_phone
(
    approved_premise_id varchar(6) not null,
    phone_id            integer    not null,
    constraint approved_premise_to_phone_to_approved_premise_fk foreign key (approved_premise_id) references approved_premise (approved_premise_id),
    constraint approved_premise_to_phone_to_phone_fk foreign key (phone_id) references phone_number (id)
);

create index approved_premise_to_phone_idx on approved_premise_to_phone (approved_premise_id, phone_id);

create table approved_premise_to_email_address
(
    approved_premise_id          varchar(6) not null,
    email_address_id integer    not null,
    constraint approved_premise_to_email_address_to_approved_premise_fk foreign key (approved_premise_id) references approved_premise (approved_premise_id),
    constraint approved_premise_to_email_address_to_email_address_fk foreign key (email_address_id) references email_address (id)
);

create index approved_premise_to_email_address_idx on approved_premise_to_email_address (approved_premise_id, email_address_id);


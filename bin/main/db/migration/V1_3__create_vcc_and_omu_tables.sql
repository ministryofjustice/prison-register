create table videolink_conferencing_centre (
    prison_id varchar(6) NOT NULL PRIMARY KEY,
    email_address varchar(256) NOT NULL,
    constraint videolink_conferencing_centre_prison_fk foreign key (prison_id) references prison(prison_id)
);

create table offender_management_unit (
   prison_id varchar(6) NOT NULL PRIMARY KEY,
   email_address varchar(256) NOT NULL,
   constraint offender_management_unit_prison_fk foreign key (prison_id) references prison(prison_id)
)
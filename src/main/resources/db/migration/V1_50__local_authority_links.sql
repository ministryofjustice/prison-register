
alter table probation_office add column local_authority varchar(12);
alter table probation_office add constraint probation_office_local_authority_fk foreign key (local_authority) references local_authority (code) on delete restrict;

alter table police_custody_suite add column local_authority varchar(12);
alter table police_custody_suite add constraint police_custody_suite_local_authority_fk foreign key (local_authority) references local_authority (code) on delete restrict;

alter table approved_premise add column local_authority varchar(12);
alter table approved_premise add constraint approved_premise_local_authority_fk foreign key (local_authority) references local_authority (code) on delete restrict;

alter table hospital add column local_authority varchar(12);
alter table hospital add constraint hospital_local_authority_fk foreign key (local_authority) references local_authority (code) on delete restrict;

alter table agency add column local_authority varchar(12);
alter table agency add constraint agency_local_authority_fk foreign key (local_authority) references local_authority (code) on delete restrict;


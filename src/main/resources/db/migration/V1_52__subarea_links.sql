
alter table probation_office add column subarea varchar(12);
alter table probation_office add constraint probation_office_subarea_fk foreign key (subarea) references subarea (code) on delete restrict;

alter table court add column  geographical_area varchar(12);
alter table court add constraint court_geographical_area_fk foreign key (geographical_area) references area (code);


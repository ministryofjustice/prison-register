alter table court
    add column local_authority varchar(12),
    add constraint court_local_authority_fk foreign key (local_authority) references local_authority (code);

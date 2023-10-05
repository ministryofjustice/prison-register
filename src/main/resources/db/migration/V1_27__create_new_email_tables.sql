CREATE TABLE email_address
(
    id      serial          NOT NULL PRIMARY KEY,
    value   varchar(100)    NOT NULL
);

CREATE TABLE contact_details
(
    id                  serial                              NOT NULL PRIMARY KEY,
    prison_id           varchar(6)                          NOT NULL,
    department_type        varchar(40)                         NOT NULL,
    create_timestamp    timestamp default current_timestamp,
    modify_timestamp    timestamp default current_timestamp,
    CONSTRAINT          contact_details_unique_constraint   UNIQUE (prison_id, department_type),
    CONSTRAINT          fk_contact_details_to_prison        FOREIGN KEY (prison_id) REFERENCES prison (prison_id)
);


CREATE TABLE contact_details_to_email_address
(
    contact_details_id integer NOT NULL,
    email_address_id   integer NOT NULL,
    CONSTRAINT contact_details_to_email_address_pkey PRIMARY KEY (contact_details_id),
    CONSTRAINT fk_contact_details_email_join FOREIGN KEY (contact_details_id) REFERENCES contact_details (id),
    CONSTRAINT fk_join_to_email_address FOREIGN KEY (email_address_id) REFERENCES email_address (id)
);

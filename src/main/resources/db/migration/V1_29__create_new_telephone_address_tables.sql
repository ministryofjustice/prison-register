CREATE TABLE telephone_address
(
    id          serial                      NOT NULL PRIMARY KEY,
    value       varchar(100)                NOT NULL,
    CONSTRAINT  telephone_address_unique_constraint     UNIQUE (value)
);

CREATE TABLE contact_details_to_telephone_address
(
    contact_details_id integer NOT NULL,
    telephone_address_id   integer NOT NULL,
    CONSTRAINT contact_details_to_telephone_address_pkey PRIMARY KEY (contact_details_id),
    CONSTRAINT fk_contact_details_telephone_address_join FOREIGN KEY (contact_details_id) REFERENCES contact_details (id),
    CONSTRAINT fk_join_to_telephone_address FOREIGN KEY (telephone_address_id) REFERENCES telephone_address (id)
);

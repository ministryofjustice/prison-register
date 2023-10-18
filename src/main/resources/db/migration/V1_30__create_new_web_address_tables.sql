CREATE TABLE web_address
(
    id          serial                      NOT NULL PRIMARY KEY,
    value       varchar(100)                NOT NULL,
    CONSTRAINT  web_address_unique_constraint     UNIQUE (value)
);

CREATE TABLE contact_details_to_web_address
(
    contact_details_id integer NOT NULL,
    web_address_id   integer NOT NULL,
    CONSTRAINT contact_details_to_web_address_pkey PRIMARY KEY (contact_details_id),
    CONSTRAINT fk_contact_details_web_address_join FOREIGN KEY (contact_details_id) REFERENCES contact_details (id),
    CONSTRAINT fk_join_to_web_address FOREIGN KEY (web_address_id) REFERENCES web_address (id)
);

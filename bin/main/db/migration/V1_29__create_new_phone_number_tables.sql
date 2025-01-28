CREATE TABLE phone_number
(
    id          serial                      NOT NULL PRIMARY KEY,
    value       varchar(100)                NOT NULL,
    CONSTRAINT  phone_number_unique_constraint     UNIQUE (value)
);

CREATE TABLE contact_details_to_phone_number
(
    contact_details_id integer NOT NULL,
    phone_number_id   integer NOT NULL,
    CONSTRAINT contact_details_to_phone_number_pkey PRIMARY KEY (contact_details_id),
    CONSTRAINT fk_contact_details_phone_number_join FOREIGN KEY (contact_details_id) REFERENCES contact_details (id),
    CONSTRAINT fk_join_to_phone_number FOREIGN KEY (phone_number_id) REFERENCES phone_number (id)
);

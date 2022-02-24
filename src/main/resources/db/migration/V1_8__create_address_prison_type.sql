CREATE TABLE address
(
    id              SERIAL      PRIMARY KEY,
    prison_id       VARCHAR(6)  NOT NULL,
    address_line1   VARCHAR(80),
    address_line2   VARCHAR(80),
    town            VARCHAR(80) NOT NULL,
    county          VARCHAR(80),
    postcode        VARCHAR(8)  NOT NULL,
    country         VARCHAR(16) NOT NULL,
    CONSTRAINT address_prison_fk FOREIGN KEY (prison_id) REFERENCES prison (prison_id)
);

CREATE TABLE prison_type
(
    id              SERIAL     PRIMARY KEY,
    prison_id       VARCHAR(6) NOT NULL,
    type            VARCHAR(6) NOT NULL,
    CONSTRAINT prison_type_prison_fk FOREIGN KEY (prison_id) REFERENCES prison (prison_id)
);

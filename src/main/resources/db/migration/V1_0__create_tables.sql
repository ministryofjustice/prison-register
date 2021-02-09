CREATE TABLE prison
(
    prison_id VARCHAR(6)  NOT NULL PRIMARY KEY,
    name      VARCHAR(40) NOT NULL,
    active    BOOLEAN     NOT NULL
);

CREATE TABLE prison_gp_practice
(
    prison_id        VARCHAR(6) NOT NULL,
    gp_practice_code VARCHAR(6) NOT NULL,
    CONSTRAINT prison_gp_practice_prison_fk FOREIGN KEY (prison_id) REFERENCES prison (prison_id)
);

CREATE UNIQUE INDEX prison_gp_practice_idx ON prison_gp_practice (prison_id, gp_practice_code);
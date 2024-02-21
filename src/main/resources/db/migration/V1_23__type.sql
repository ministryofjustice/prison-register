CREATE TABLE type
(
    id          VARCHAR(6)  NOT NULL PRIMARY KEY,
    description VARCHAR(80) NOT NULL
);

INSERT INTO type
VALUES ('HMP', 'His Majesty’s Prison (HMP)'),
       ('YOI', 'His Majesty’s Youth Offender Institution (YOI)'),
       ('IRC', 'Immigration Removal Centre (IRC)'),
       ('STC', 'Secure Training Centre (STC)'),
       ('YCS', 'Youth Custody Service (YCS)');

ALTER TABLE prison_type ADD CONSTRAINT prison_type_type_fk FOREIGN KEY(TYPE) REFERENCES type(id);

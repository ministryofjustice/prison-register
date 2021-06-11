INSERT INTO prison VALUES ('FYI', 'Feltham A (HMPYOI)', true);

UPDATE prison set name = 'Hewell (HMP)' where prison_id = 'HEI';
UPDATE prison set name = 'Medway (STC)' where prison_id = 'MWI';

INSERT INTO prison_gp_practice VALUES ('KTI', 'Y03175');
INSERT INTO prison_gp_practice VALUES ('MWI', 'Y04619');
INSERT INTO prison_gp_practice VALUES ('FYI', 'Y06017');
INSERT INTO prison_gp_practice VALUES ('PFI', 'Y03149');

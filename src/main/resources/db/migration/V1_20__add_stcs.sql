-- Rainsbrook STC  - currently closed
INSERT INTO PRISON (PRISON_ID, NAME, MALE, FEMALE, CONTRACTED, ACTIVE)
  VALUES ('STC2', 'Rainsbrook (STC)', true, true, true, false);

INSERT INTO PRISON_TYPE (PRISON_ID, TYPE) VALUES ('STC2', 'STC');

INSERT INTO ADDRESS (PRISON_ID, ADDRESS_LINE1, TOWN,  COUNTRY, POSTCODE)
  VALUES ('STC2', 'Rainsbrook Secure Training Centre', 'Willoughby', 'England', 'CV23 8SY');


-- Oakhill STC
INSERT INTO PRISON (PRISON_ID, NAME, MALE, CONTRACTED, ACTIVE)
  VALUES ('STC3', 'Oakhill (STC)', true, true, true);

INSERT INTO PRISON_TYPE (PRISON_ID, TYPE) VALUES ('STC3', 'STC');

INSERT INTO ADDRESS (PRISON_ID, ADDRESS_LINE1, ADDRESS_LINE2, TOWN,  COUNTRY, POSTCODE)
  VALUES ('STC3', 'Chalgrove Field', 'Oakhill', 'Milton Keynes','England', 'MK5 6AJ');

UPDATE prison SET prison_name_in_welsh = 'Carchar Brynbuga' WHERE prison_id = 'UKI';
UPDATE prison SET prison_name_in_welsh = 'Carchar y Berwyn' WHERE prison_id = 'BWI';
UPDATE prison SET prison_name_in_welsh = 'Carchar Caerdydd' WHERE prison_id = 'CFI';
UPDATE prison SET prison_name_in_welsh = 'Carchar Prescoed' WHERE prison_id = 'UPI';
UPDATE prison SET prison_name_in_welsh = 'Carchar Parc' WHERE prison_id = 'PRI';
UPDATE prison SET prison_name_in_welsh = 'Sefydliad Troseddwyr Ifanc y Parc' WHERE prison_id = 'PYI';
UPDATE prison SET prison_name_in_welsh = 'Carchar Abertawe' WHERE prison_id = 'SWI';

UPDATE address SET
address_line1_in_welsh = '47 Maryport Street',
town_in_welsh = 'Brynbuga',
county_in_welsh = 'Sir Fynwy',
country_in_welsh = ' Cymru'
WHERE prison_id = 'UKI';

UPDATE address SET
address_line1_in_welsh = 'Ffordd y Bont Ystâd Ddiwydiannol Wrecsam',
town_in_welsh = 'Wrecsam',
county_in_welsh = 'Gogledd Cymru',
country_in_welsh = ' Cymru'
WHERE prison_id = 'BWI';

UPDATE address SET
address_line1_in_welsh = 'Heol Knox',
town_in_welsh = 'Caerdydd',
country_in_welsh = ' Cymru'
WHERE prison_id = 'CFI';

UPDATE address SET
address_line1_in_welsh = 'Coed-y-Paen',
town_in_welsh = 'Pont-y-pŵl',
county_in_welsh = 'Sir Fynwy',
country_in_welsh = ' Cymru'
WHERE prison_id = 'UKI';

UPDATE address SET
address_line1_in_welsh = 'Heol Hopcyn John',
town_in_welsh = 'Coety, Pen-y-bont',
country_in_welsh = ' Cymru'
WHERE prison_id = 'PRI';

UPDATE address SET
address_line1_in_welsh = 'Ffordd y Bont Ystâd Ddiwydiannol Wrecsam',
town_in_welsh = 'Wrecsam',
country_in_welsh = ' Cymru'
WHERE prison_id = 'PYI';

UPDATE address SET
address_line1_in_welsh = 'Heol Knox',
town_in_welsh = 'Caerdydd',
country_in_welsh = ' Cymru'
WHERE prison_id = 'SWI';


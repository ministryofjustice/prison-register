-- Aylesbury is HMYOI on spreadsheet but not marked as Youth Custody Estate - need to confirm
UPDATE PRISON_TYPE SET TYPE = 'YCS' WHERE PRISON_ID = 'AYI';

-- Cookham Wood
UPDATE PRISON_TYPE SET TYPE = 'YCS' WHERE PRISON_ID = 'CKI';

-- Feltham
UPDATE PRISON_TYPE SET TYPE = 'YCS' WHERE PRISON_ID = 'FYI';

-- Werrington
UPDATE PRISON_TYPE SET TYPE = 'YCS' WHERE PRISON_ID = 'WNI';

-- Wetherby
UPDATE PRISON_TYPE SET TYPE = 'YCS' WHERE PRISON_ID = 'WYI';


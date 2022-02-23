ALTER TABLE prison
    ADD COLUMN  description     VARCHAR(256)    NULL;

ALTER TABLE prison
    ADD COLUMN  gender          VARCHAR(40)     NULL;

ALTER TABLE prison
    ADD COLUMN  inactive_date   DATE            NULL;

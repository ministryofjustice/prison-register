ALTER TABLE prison
    ADD COLUMN  description     VARCHAR(256)    NULL;

ALTER TABLE prison
    ADD COLUMN  male            BOOLEAN         NOT NULL    DEFAULT false;

ALTER TABLE prison
    ADD COLUMN  female          BOOLEAN         NOT NULL    DEFAULT false;

ALTER TABLE prison
    ADD COLUMN  inactive_date   DATE            NULL;

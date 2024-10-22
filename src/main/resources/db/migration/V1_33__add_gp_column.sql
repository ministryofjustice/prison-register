ALTER TABLE prison   ADD COLUMN  gp_practice_code            varchar(6);

CREATE INDEX prison_gp_practice_code_idx ON prison (gp_practice_code);

UPDATE prison p
set gp_practice_code = (select gp_practice_code from prison_gp_practice gp where gp.prison_id = p.prison_id)
WHERE EXISTS (select 1 from prison_gp_practice gp2 where gp2.prison_id = p.prison_id);

DROP TABLE prison_gp_practice;




CREATE TABLE prison_category
(
    category TEXT NOT NULL,
    prison_id VARCHAR NOT NULL,
    CONSTRAINT prison_category_prison_type_fk FOREIGN KEY (prison_id) REFERENCES prison(prison_id)
);
CREATE INDEX prison_category_prison_id ON prison_category(prison_id);
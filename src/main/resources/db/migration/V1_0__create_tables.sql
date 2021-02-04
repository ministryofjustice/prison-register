CREATE TABLE court
(
    id                VARCHAR(6)  NOT NULL PRIMARY KEY,
    court_name        VARCHAR(80) NOT NULL,
    court_description VARCHAR(200),
    active    BOOLEAN     NOT NULL
);

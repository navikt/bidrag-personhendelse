-- Table: shedlock
-- DROP TABLE shedlock;

CREATE TABLE shedlock
(
    name VARCHAR(64),
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255),
    PRIMARY KEY (name)
)
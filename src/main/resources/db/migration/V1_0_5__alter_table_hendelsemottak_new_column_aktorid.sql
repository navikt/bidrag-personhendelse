-- Table: hendelsemottak

ALTER TABLE hendelsemottak
    ADD COLUMN aktorid varchar(13);

/*** Rulle tilbake ***

ALTER TABLE hendelsemottak
    DROP COLUMN aktorid

delete from flyway_schema_history where version = '1.0.5';

 */
-- Table: hendelsemottak

update hendelsemottak set aktorid = substring(personidenter, '\d{13}');

/*** Rulle tilbake ***

update hendelsemottak set aktorid = null;
delete from flyway_schema_history where version = '1.0.6';

 */
alter table hendelsemottak add column aktorid varchar(13);
update hendelsemottak set aktorid = substring(personidenter, '\d{13}')

/*** rulle tilbake ***

  alter table hendelsemottak drop column aktorid

  delete from flyway_schema_history where version = '´2.0.1';

 */
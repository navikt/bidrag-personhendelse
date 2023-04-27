-- table: hendelsemottak

alter table hendelsemottak add column aktor_id integer;

/*** rulle tilbake ***

  alter table hendelsemottak drop column aktor_id
  delete from flyway_schema_history where version = '1.0.6';

 */
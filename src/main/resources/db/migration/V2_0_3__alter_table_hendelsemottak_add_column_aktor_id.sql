-- table: hendelsemottak

alter table hendelsemottak
    drop column aktorid,
    add column aktor_id integer;

/*** rulle tilbake ***

  alter table hendelsemottak
    drop column aktor_id,
    add column aktorid varchar(13);

  delete from flyway_schema_history where version = '2.0.3';

 */
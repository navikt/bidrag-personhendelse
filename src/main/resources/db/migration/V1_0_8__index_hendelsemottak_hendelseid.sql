-- Table: hendelsemottak

create index if not exists index_hendelsemottak_hendelseid on hendelsemottak(hendelseid)

/*** Rulle tilbake ***
  drop index index_hendelsemottak_hendelseid;
  delete from flyway_schema_history where version = '1.0.8';
 */
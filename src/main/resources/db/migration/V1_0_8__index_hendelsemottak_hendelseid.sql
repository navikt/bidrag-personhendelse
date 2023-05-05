-- Table: hendelsemottak

create index if not exists index_hendelsemottak_hendelseid on hendelsemottak(hendelseid)

/*** Rulle tilbake ***
  DROP INDEX INDEX_HENDELSEMOTTAK_HENDELSEID;
  delete from flyway_schema_history where version = '1.0.8';
 */
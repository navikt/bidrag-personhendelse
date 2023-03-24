-- Table: hendelsemottak

CREATE INDEX IF NOT EXISTS INDEX_HENDELSEMOTTAK_HENDELSEID ON HENDELSEMOTTAK(HENDELSEID)

/*** Rulle tilbake ***
  DROP INDEX INDEX_HENDELSEMOTTAK_HENDELSEID;
  delete from flyway_schema_history where version = '1.0.3';
 */
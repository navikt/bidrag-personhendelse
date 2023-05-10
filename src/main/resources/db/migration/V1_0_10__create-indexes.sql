create index if not exists index_hendelsemottak_status on hendelsemottak(status);
create index if not exists index_aktor_publisert on aktor(publisert);

/*** rulle tilbake ***

  drop index index_hendelsemottak_status;
  drop index index_aktor_publisert;
  delete from flyway_schema_history where version = '1.0.10';

 */
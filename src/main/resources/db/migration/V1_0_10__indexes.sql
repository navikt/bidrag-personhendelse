create index if not exists index_hendelsemottak_status on hendelsemottak(status);
create index if not exists index_kontoendring_status on kontoendring(status);
create index if not exists index_aktor_publisert on aktor(publisert);

drop index index_kontoendring_fk_aktor;
create index index_kontoendring_aktor_id on kontoendring(aktor_id)

/*** rulle tilbake ***

  drop index index_hendelsemottak_status;
  drop index index_kontoendring_status;
  drop index index_aktor_publisert;
  drop index index_kontoendring_aktor_id;
  create index index_kontoendring_fk_aktor on kontoendring(aktor_id)

  delete from flyway_schema_history where version = '1.0.10';

 */
-- table: hendelsemottak

alter table hendelsemottak
    add column aktor_id integer,
    add constraint fk_aktor foreign key (aktor_id)
        references aktor (id) match simple
        on update no action
        on delete no action;

create index if not exists index_hendelsemottak_fk_aktor on hendelsemottak(aktor_id)

/*** rulle tilbake ***

alter table hendelsemottak
  drop column aktor_id,
  drop constraint fk_aktor;

drop index index_hendelsemottak_fk_aktor;

delete from flyway_schema_history where version = '1.0.5';

 */
alter table hendelsemottak
    add constraint fk_aktor foreign key (aktor_id)
        references aktor (id) match simple
        on update no action
        on delete no action,
    add constraint hendelsemottak_pkey primary key (id);

create index index_hendelsemottak_aktor_id on hendelsemottak(aktor_id);

/*** Rulle tilbake **

  alter table hendelsemottak drop constraint fk_aktor;
  drop index index_hendelsemottak_aktor_id;
  delete from flyway_schema_history where version = '1.0.8';

 */


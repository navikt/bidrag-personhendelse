/***

 table: hendelsemottak

 Prerequisite:
 Stop all bidrag-person-hendelse pods prior to deploy using the kubectl command:
  -> k scale deployment bidrag-person-hendelse --replicas 0

  This script will create a foreign key constraint for column aktor_id in hendelsemottak with reference to
  to the id field of table aktor.

  Population of the aktor_id column happens in a series of steps to optimize performance.

***/

insert into aktor(aktorid) select distinct substring(personidenter, '\d{13}') from  hendelsemottak;

create table mellomlagring(
    hendelsemottak_id integer not null,
    aktor_id integer,
    aktorid varchar(13)
);

insert into mellomlagring(hendelsemottak_id, aktorid) select id, substring(personidenter, '\d{13}') from  hendelsemottak;

create table hm as (
    select hendelseid, opplysningstype, endringstype, personidenter, tidligere_hendelseid, hendelse, master, offset_pdl, status, statustidspunkt, opprettet, h.id, aktor.id as aktor_id
    from hendelsemottak h
             inner join mellomlagring ml on ml.hendelsemottak_id = h.id
             inner join aktor on aktor.aktorid = ml.aktorid
);

alter table hendelsemottak rename to hendelsemottak_gammel;
alter table hm rename to hendelsemottak;

drop table mellomlagring;

/*** rulle tilbake ***

drop table hendelsemottak;

alter table hendelsemottak_gammel rename to hendelsemottak;

delete from aktor;

delete from flyway_schema_history where version = '1.0.7';

 */
/*
 Oppdatere aktor-tabellen med aktørid fra hendelsemottak,
 oppdatere hendelsemottak med fremmednøkkel til aktør.

 Legger til en midlertidig hjelpekolonne i hendelsemottak for aktørid for økt ytelse.
 */

alter table hendelsemottak add column aktorid varchar(13);
create index index_hendelsemottak_aktor_id on hendelsemottak(aktor_id);
update hendelsemottak set aktorid = substring(personidenter, '\d{13}');

insert into aktor (aktorid) select distinct aktorid from  hendelsemottak;

update hendelsemottak set aktor_id = aktor.id
from hendelsemottak hm
inner join aktor on aktor.aktorid = hm.aktorid

/*** rulle tilbake ***

delete from aktor;
update hendelsemottak set aktor_id = null;
delete from flyway_schema_history where version = '1.0.7';

 */
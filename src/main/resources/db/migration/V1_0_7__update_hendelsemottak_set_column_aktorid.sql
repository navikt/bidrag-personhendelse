/*
 Oppdatere aktor-tabellen med aktørid fra hendelsemottak,
 oppdatere hendelsemottak med fremmednøkkel til aktør
 */

insert into aktor (aktorid) select distinct substring(personidenter, '\d{13}') from  hendelsemottak;

update hendelsemottak set aktor_id =
        (select id from aktor where aktorid in (select distinct substring(personidenter, '\d{13}') from hendelsemottak));

/*** rulle tilbake ***

delete from aktor;
update hendelsemottak set aktor_id = null;
delete from flyway_schema_history where version = '1.0.7';

 */
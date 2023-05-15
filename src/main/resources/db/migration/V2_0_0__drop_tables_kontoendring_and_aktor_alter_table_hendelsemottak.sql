alter table hendelsemottak drop column aktor_id;
drop table kontoendring;
drop table aktor;

/*** rulle tilbake ***

  Kjøre flyway versjon 1.0.4 tom 1.0.10 kronologisk, deretter:

  delete from flyway_schema_history where version = '´2.0.0';

 */
# bidrag-person-hendelse
Mottaksapplikasjon for personhendelser. Lytter på ulike hendelser fra PDL (fødsler, dødsfall mm). Distribuerer disse videre til Bisys via MQ. 

## Monitorering

#### Elastic
Applikasjonen logger til Elastic, eksempel på loggsøk:
[Skedulert overføring - sjekke identifisert antall meldinger](https://logs.adeo.no/app/r/s/dNtSM)

#### Prometheus
Mottatte meldinger telles etter opplysning- og endringstype. Bare opplysningstyper definert i Livshendelse.Opplysningstype telles, øvrige meldinger ignoreres. 
Eksempel på opptelling i Prometheus:
[bostedsadresse](https://prometheus.prod-gcp.nav.cloud.nais.io/graph?g0.expr=bidrag_personhendelse_bostedsadresse_opprettet_total&g0.tab=1&g0.stacked=0&g0.show_exemplars=0&g0.range_input=1h&g0.end_input=2023-03-14%2009%3A27%3A32&g0.moment_input=2023-03-14%2009%3A27%3A32&g1.expr=bidrag_personhendelse_bostedsadresse_korrigert_total&g1.tab=1&g1.stacked=0&g1.show_exemplars=0&g1.range_input=1h&g1.end_input=2023-03-14%2009%3A27%3A32&g1.moment_input=2023-03-14%2009%3A27%3A32&g2.expr=bidrag_personhendelse_bostedsadresse_opphoert_total&g2.tab=1&g2.stacked=0&g2.show_exemplars=0&g2.range_input=1h&g2.end_input=2023-03-14%2008%3A28%3A08&g2.moment_input=2023-03-14%2008%3A28%3A08&g3.expr=bidrag_personhendelse_bostedsadresse_annullert_total&g3.tab=1&g3.stacked=0&g3.show_exemplars=0&g3.range_input=1h&g3.end_input=2023-03-14%2009%3A59%3A03&g3.moment_input=2023-03-14%2009%3A59%3A03)

## Lokal kjøring
Appen bygges med maven og kan kjøres fra DevLauncher-klassen. Sett `-Dspring.profiles.active=dev` under Edit Configurations -> VM Options. Lokalt må man kjøre serveren sammen med [navkafka-docker-compose][1].

Oppsummert oppstartsprosedyre for navkafka-docker-compose testmiljø med Colima Docker for Mac (bruk dedikert terminal-vindu/ tab):
```
 > colima start
 > docker-compose build
 > docker-compose up

```


Topicene vi lytter på må da opprettes via deres api med følgende data:
```
{
  "topics": [
    {
      "topicName": "aapen-person-pdl-leesah-v1",
      "members": [
        {"member":"srvc01", "role":"CONSUMER"}
      ],
      "numPartitions": 3
    },
    {
      "topicName": "aapen-dok-journalfoering-v1-q1",
      "members": [
        {"member":"srvc01", "role":"CONSUMER"}
      ],
      "numPartitions": 3
    },
    {
      "topicName": "aapen-person-pdl-aktor-v1",
      "members": [
        {"member":"srvc01", "role":"CONSUMER"}
      ],
      "numPartitions": 3
    },
  ]
}
```
Dette kan for eksempel gjøres med følgende kommandoer:
```
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-person-pdl-leesah-v1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 3 }" \
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-dok-journalfoering-v1-q1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 3 }" \
curl -X POST "http://igroup:itest@localhost:8840/api/v1/topics" -H "Accept: application/json" -H "Content-Type: application/json" --data "{"name": "aapen-person-pdl-aktor-v1", "members": [{ "member": "srvc01", "role": "CONSUMER" }], "numPartitions": 3 }"
```

Se README i navkafka-docker-compose for mer info om hvordan man kjører den og kaller apiet.

## Produksjonssetting
Appen blir produksjonssatt ved push til release

## Henvendelser
For NAV-interne kan henvendelser rettes til #bidrag på slack. Ellers kan henvendelser rettes via et issue her på github-repoet.

[1]: https://github.com/navikt/navkafka-docker-compose

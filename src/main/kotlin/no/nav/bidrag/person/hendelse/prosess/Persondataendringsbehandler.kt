package no.nav.bidrag.person.hendelse.prosess

import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.StatusKontoendring
import org.springframework.stereotype.Service

@Service
class Persondataendringsbehandler(val databasetjeneste: Databasetjeneste) {

    var kontoeiereMedOppdatertKontoinformasjon = databasetjeneste.henteKontoeiere(StatusKontoendring.MOTTATT)

    // var t = databasetjeneste.hentePersonerIOverførteLivshendelser()
}

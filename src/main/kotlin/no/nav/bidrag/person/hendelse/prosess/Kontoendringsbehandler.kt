package no.nav.bidrag.person.hendelse.prosess

import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.BidragPersonklient
import no.nav.bidrag.person.hendelse.integrasjon.pdl.domene.Identgruppe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Kontoendringsbehandler(val bidragPersonklient: BidragPersonklient, val databasetjeneste: Databasetjeneste) {

    fun lagreKontoendring(personidentKontoeier: String) {
        val alleIdenterKontoeier = bidragPersonklient.henteAlleIdenterForPerson(personidentKontoeier)
        val aktørid = alleIdenterKontoeier.find { it.gruppe.equals(Identgruppe.AKTORID) }?.ident
        if (aktørid != null) {
            databasetjeneste.lagreKontoendring(aktørid)
        } else {
            log.warn("Aktørid null for kontoeier - kontoendring ble ikke lagret.")
            slog.warn("Aktørid null for kontoeier $personidentKontoeier - kontoendring ble ikke lagret")
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
        val slog: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
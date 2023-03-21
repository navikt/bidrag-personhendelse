package no.nav.bidrag.person.hendelse.prosess

import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.exception.OverføringFeiletException
import no.nav.bidrag.person.hendelse.integrasjon.distribusjon.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class Meldingstjeneste(
    open val databasetjeneste: Databasetjeneste,
    open val egenskaper: Egenskaper,
    open val meldingsprodusent: Meldingsprodusent
) {

    @Transactional
    open fun sendeMeldinger(meldingsider: List<Long>): Int {
        var antallOverført = 0
        for (id in meldingsider.iterator()) {
            var mottattHendelse = databasetjeneste.henteHendelse(id)
            if (mottattHendelse.isPresent) {
                try {
                    meldingsprodusent.sendeMelding(egenskaper.wmq.queueNameLivshendelser, mottattHendelse.get().hendelse)
                    databasetjeneste.oppdatereStatus(id, Status.OVERFØRT)
                    antallOverført++
                } catch (ofe: OverføringFeiletException) {
                    databasetjeneste.oppdatereStatus(id, Status.OVERFØRING_FEILET)
                }
            }
        }

        return antallOverført
    }
}
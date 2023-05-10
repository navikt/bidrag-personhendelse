package no.nav.bidrag.person.hendelse.skedulering

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene.Endringsmelding
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PublisereEndringsmeldinger(
    val bidragtopic: BidragKafkaMeldingsprodusent,
    val databasetjeneste: Databasetjeneste,
    val egenskaper: Egenskaper
) {
    @Transactional
    @Scheduled(cron = "\${publisere_personhendelser.kjøreplan}")
    @SchedulerLock(
        name = "publisere_personhendelser",
        lockAtLeastFor = "\${publisere_personhendelser.lås.min}",
        lockAtMostFor = "\${publisere_personhendelser.lås.max}"
    )
    fun identifisereOgPublisere() {
        // Hente aktørid med personidenter til kontoeiere med nylige endringer
        val aktøriderKontoeiere = databasetjeneste.henteAktøridTilKontoeiereMedNyligeKontoendringer()
        log.info("Fant ${aktøriderKontoeiere.size} unike kontoeiere med nylige kontoendringer.")

        // Hente aktørid med personidenter til til personer med nylige endringer i personopplysninger
        val aktøriderPersonopplysninger = databasetjeneste.henteAktøridTilPersonerMedNyligOppdatertePersonopplysninger()
        log.info("Fant ${aktøriderPersonopplysninger.size} unike personer med nylige endringer i personopplysninger.")

        val aktøriderForPublisering = aktøriderPersonopplysninger.plus(aktøriderKontoeiere)
        log.info("Identifiserte totalt ${aktøriderForPublisering.size} unike personer som det skal publiseres endringsmeldinger for.")

        // Publisere melding til intern topic for samtlige personer med endringer
        aktøriderForPublisering.forEach { bidragtopic.publisereEndringsmelding(Endringsmelding(it.key, it.value)) }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

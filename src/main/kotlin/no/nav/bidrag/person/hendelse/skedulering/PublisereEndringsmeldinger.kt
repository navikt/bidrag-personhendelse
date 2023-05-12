package no.nav.bidrag.person.hendelse.skedulering

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
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
        // Hente aktør med personidenter til kontoeiere med nylige endringer
        val aktørerKontoeiere = databasetjeneste.henteAktørMedIdenterTilKontoeiereMedNyligeKontoendringer()
        log.info("Fant ${aktørerKontoeiere.size} unike kontoeiere med nylige kontoendringer.")

        // Hente aktør med personidenter til til personer med nylige endringer i personopplysninger
        val aktørerPersonopplysninger = databasetjeneste.henteAktørMedIdenterTilPersonerMedNyligOppdatertePersonopplysninger()
        log.info("Fant ${aktørerPersonopplysninger.size} unike personer med nylige endringer i personopplysninger.")

        val aktørerForPublisering = aktørerPersonopplysninger.plus(aktørerKontoeiere)
        log.info("Identifiserte totalt ${aktørerForPublisering.size} unike personer som det skal publiseres endringsmeldinger for.")

        val subsetMedAktører = aktørerForPublisering.keys.take(egenskaper.generelt.maksAntallMeldingerSomSendesTilBidragTopicOmGangen).toSet()

        if (subsetMedAktører.size < aktørerForPublisering.size) log.info("Begrenser antall meldinger som skal publiseres til ${subsetMedAktører.size}")

        // Publisere melding til intern topic for samtlige personer med endringer
        subsetMedAktører.forEach {
            bidragtopic.publisereEndringsmelding(it, aktørerForPublisering.getValue(it))
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

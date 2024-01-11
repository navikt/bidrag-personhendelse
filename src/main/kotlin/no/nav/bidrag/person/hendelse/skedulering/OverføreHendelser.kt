package no.nav.bidrag.person.hendelse.skedulering

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.exception.OverføringFeiletException
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.bisys.BisysMeldingsprodusjon
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class OverføreHendelser(
    open val databasetjeneste: Databasetjeneste,
    open val egenskaper: Egenskaper,
    open val meldingsprodusent: BisysMeldingsprodusjon,
) {
    @Transactional
    @Scheduled(cron = "\${overføre_hendelser.kjøreplan}")
    @SchedulerLock(
        name = "overføre_hendelser",
        lockAtLeastFor = "\${overføre_hendelser.lås.min}",
        lockAtMostFor = "\${overføre_hendelser.lås.max}",
    )
    fun overføreHendelserTilBisys() {
        val sisteStatusoppdateringFør =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong())
        log.info("Ser etter hendelser med status mottatt og med siste statusoppdatering før $sisteStatusoppdateringFør")

        val hendelserKlarTilOverføring =
            databasetjeneste.hendelsemottakDao.idTilHendelserSomErKlarTilOverføring(sisteStatusoppdateringFør)
        log.info(
            henteLoggmelding(
                hendelserKlarTilOverføring.size,
                egenskaper.generelt.maksAntallMeldingerSomOverfoeresTilBisysOmGangen,
            ),
        )

        // Begrenser antall hendelser som skal videresendes
        val hendelserSomOverføresIDenneOmgang =
            hendelserKlarTilOverføring.take(egenskaper.generelt.maksAntallMeldingerSomOverfoeresTilBisysOmGangen)

        try {
            val antallOverført: Int =
                meldingsprodusent.sendeMeldinger(
                    egenskaper.integrasjon.wmq.queueNameLivshendelser,
                    databasetjeneste.hendelsemottakDao.findAllById(hendelserSomOverføresIDenneOmgang).map { it.hendelse },
                )
            databasetjeneste.oppdatereStatusPåHendelser(hendelserSomOverføresIDenneOmgang, Status.OVERFØRT)
            log.info("Overføring fullført (for antall: $antallOverført)")
        } catch (ofe: OverføringFeiletException) {
            databasetjeneste.oppdatereStatusPåHendelser(hendelserSomOverføresIDenneOmgang, Status.OVERFØRING_FEILET)
            log.error("Overføring av $hendelserSomOverføresIDenneOmgang meldinger feilet")
        }
    }

    private fun henteLoggmelding(
        antallIdentifiserteHendelser: Int,
        maksAntallHendelserPerKjøring: Int,
    ): String {
        val melding =
            "Fant $antallIdentifiserteHendelser livshendelser med status MOTTATT. " +
                "Antall hendelser per kjøring er begrenset til $maksAntallHendelserPerKjøring. "
        return if (antallIdentifiserteHendelser > maksAntallHendelserPerKjøring) {
            melding + "Overfører $maksAntallHendelserPerKjøring hendelser i denne omgang."
        } else {
            if (antallIdentifiserteHendelser > 0) {
                melding + "Overfører alle de $antallIdentifiserteHendelser identifiserte hendelsene."
            } else {
                melding + "Ingen hendelser å overføre."
            }
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

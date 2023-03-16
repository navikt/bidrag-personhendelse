package no.nav.bidrag.person.hendelse.skedulering

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.exception.OverføringFeiletException
import no.nav.bidrag.person.hendelse.integrasjon.distribusjon.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
open class OverføreHendelser(
    open val databasetjeneste: Databasetjeneste,
    open val egenskaper: Egenskaper,
    open val meldingsprodusent: Meldingsprodusent
) {

    @Scheduled(cron = "\${kjøreplan.overføre_hendelser}")
    @SchedulerLock(name = "overføre_hendelser", lockAtLeastFor = "PT10M", lockAtMostFor = "PT1H")
    open fun overføreHendelserTilBisys() {
        var sisteStatusoppdateringFør = LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong())
        log.info("Ser etter hendelser med status mottatt og med siste statusoppdatering før ${sisteStatusoppdateringFør}")

        var idTilHendelserSomSkalVideresendes = databasetjeneste.henteIdTilHendelserMedStatusMottatMedStatustidspunktFør(sisteStatusoppdateringFør)
        log.info(henteLoggmelding(idTilHendelserSomSkalVideresendes.size, egenskaper.generelt.maksAntallMeldingerSomOverfoeresTilBisysOmGangen))

        // Begrenser antall, og setter status til UNDER_PROSESSERING for hendelsene som skal videresendes
        idTilHendelserSomSkalVideresendes.take(egenskaper.generelt.maksAntallMeldingerSomOverfoeresTilBisysOmGangen).forEach{
            databasetjeneste.oppdatereStatus(it, Status.UNDER_PROSESSERING)
        }

        for (id in idTilHendelserSomSkalVideresendes.iterator()) {
            var mottattHendelse = databasetjeneste.henteHendelse(id)
            if (mottattHendelse.isPresent) {
                try {
                    meldingsprodusent.sendeMelding(egenskaper.wmq.queueNameLivshendelser, mottattHendelse.get().hendelse)
                    databasetjeneste.oppdatereStatus(id, Status.OVERFØRT)
                } catch (ofe: OverføringFeiletException) {
                    databasetjeneste.oppdatereStatus(id, Status.OVERFØRING_FEILET)
                }
            }
        }

        if (idTilHendelserSomSkalVideresendes.isNotEmpty()) log.info("Overføring fullført (for antall: ${idTilHendelserSomSkalVideresendes.size}")
    }

    private fun henteLoggmelding(antallIdentifiserteHendelser: Int, maksAntallHendelserPerKjøring: Int): String {
        var melding =  "Fant ${antallIdentifiserteHendelser} livshendelser med status MOTTATT. Antall hendelser per kjøring er begrenset til ${maksAntallHendelserPerKjøring}."
        if (antallIdentifiserteHendelser > maksAntallHendelserPerKjøring) {
            return melding + "Overfører ${maksAntallHendelserPerKjøring} hendelser i denne omgang."
        } else {
            return melding + "Overfører alle de ${antallIdentifiserteHendelser} identifiserte hendelsene."
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(OverføreHendelser::class.java)
    }
}
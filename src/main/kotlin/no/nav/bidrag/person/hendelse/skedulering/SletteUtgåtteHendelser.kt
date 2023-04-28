package no.nav.bidrag.person.hendelse.skedulering

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SletteUtgåtteHendelser(
    open val databasetjeneste: Databasetjeneste,
    open val egenskaper: Egenskaper

) {
    @Scheduled(cron = "\${slette_hendelser.kjøreplan}")
    @SchedulerLock(
        name = "slette_hendelser",
        lockAtLeastFor = "\${slette_hendelser.lås.min}",
        lockAtMostFor = "\${slette_hendelser.lås.max}"
    )
    fun sletteUtgåtteHendelserFraDatabase() {
        var statusoppdateringFør = LocalDate.now().atStartOfDay()
            .minusDays(egenskaper.generelt.antallDagerLevetidForUtgaatteHendelser.toLong())

        log.info("Ser etter utgåtte livshendelser med siste statusoppdatering før $statusoppdateringFør som skal slettes fra databasen.")

        var kansellerteHendelser =
            databasetjeneste.hendelsemottakDao.henteIdTilHendelser(Status.KANSELLERT, statusoppdateringFør)
        var overførteHendelser =
            databasetjeneste.hendelsemottakDao.henteIdTilHendelser(Status.OVERFØRT, statusoppdateringFør)

        log.info("Fant ${kansellerteHendelser.size} kansellerte, og ${overførteHendelser.size} overførte hendelser som skal slettes fra databasen")

        var antallSlettedeKansellerteHendelser = sletteHendelser(kansellerteHendelser, "kansellerte")
        log.info("Totalt ble $antallSlettedeKansellerteHendelser av ${kansellerteHendelser.size} identifiserte kansellerte hendelser slettet")

        var antallSLettedeOverførteHendelser = sletteHendelser(overførteHendelser, "overførte")
        log.info("Totalt ble $antallSLettedeOverførteHendelser av ${overførteHendelser.size} identifiserte overførteHendelser hendelser slettet")

        if (kansellerteHendelser.size.toLong() + overførteHendelser.size.toLong() == antallSlettedeKansellerteHendelser + antallSLettedeOverførteHendelser) {
            log.info("Alle de identifiserte hendelsene ble slettet.")
        } else {
            log.warn("Ikke alle de identifiserte hendelsene ble slettet.")
        }
    }

    private fun sletteHendelser(ider: Set<Long>, hendelsebeskrivelse: String): Long {
        if (ider.size > egenskaper.generelt.bolkstoerrelseVedSletting) {
            log.info("Antall $hendelsebeskrivelse-hendelser identifisert for sletting oversteg grensen på ${egenskaper.generelt.bolkstoerrelseVedSletting}.")
            var listeMedListeAvHendelseider = ider.chunked(egenskaper.generelt.bolkstoerrelseVedSletting)

            var totaltAntallHendelserSomBleSlettet: Long = 0
            var bolknummer: Int = 1
            listeMedListeAvHendelseider.forEach {
                log.info("Sletter bolk-$bolknummer med ${it.size} $hendelsebeskrivelse-hendelser.")
                var antallHendelserSomBleSlettet = databasetjeneste.hendelsemottakDao.deleteByIdIn(it.toSet())
                log.info("$antallHendelserSomBleSlettet av ${it.size} $hendelsebeskrivelse-hendelser i bolk-$bolknummer ble slettet.")
                totaltAntallHendelserSomBleSlettet += antallHendelserSomBleSlettet
                bolknummer++
            }

            return totaltAntallHendelserSomBleSlettet
        } else {
            return databasetjeneste.hendelsemottakDao.deleteByIdIn(ider)
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

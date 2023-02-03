package no.nav.bidrag.person.hendelse.skedulering

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.integrasjon.distribusjon.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import no.nav.bidrag.person.hendelse.prosess.LocalDateTimeTypeAdapter
import no.nav.bidrag.person.hendelse.prosess.LocalDateTypeAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
open class OverføreHendelser(
    open val databasetjeneste: Databasetjeneste,
    open val egenskaper: Egenskaper,
    open val meldingsprodusent: Meldingsprodusent
) {

    @Scheduled(cron = "\${kjøreplan.overføre_hendelser}")
    @SchedulerLock(name = "hendelser_til_bisys", lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
    fun overføreHendelserTilBisys() {
        log.info("Ser etter livshendelser som skal overføres til Bisys")

        var sisteStatusoppdateringFør = LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong())
        log.info("Ser etter hendelser med status mottatt og med siste statusoppdatering før {}", sisteStatusoppdateringFør)

        var idTilHendelserSomSkalVideresendes = databasetjeneste.henteIdTilHendelserSomSkalOverføresTilBisys(sisteStatusoppdateringFør)
        log.info("Antall livshendelser som skal overføres: {}", idTilHendelserSomSkalVideresendes.size)

        for (id in idTilHendelserSomSkalVideresendes.iterator()) {
            var mottattHendelse = databasetjeneste.henteHendelse(id)
            meldingsprodusent.sendeMelding(egenskaper.wmq.queueNameLivshendelser, oppretteGson().toJson(mottattHendelse.hendelse))
            mottattHendelse.statustidspunkt = LocalDateTime.now()
            mottattHendelse.status = Status.OVERFØRT
        }

        if (idTilHendelserSomSkalVideresendes.size > 0) log.info("Overføring fullført (for antall: {})", idTilHendelserSomSkalVideresendes.size)
    }

    private fun oppretteGson(): Gson {
        var gsonbuilder = GsonBuilder()
        gsonbuilder.registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter().nullSafe())
        gsonbuilder.registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter().nullSafe())
        var gson = gsonbuilder.create()
        return gson
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(OverføreHendelser::class.java)
    }
}
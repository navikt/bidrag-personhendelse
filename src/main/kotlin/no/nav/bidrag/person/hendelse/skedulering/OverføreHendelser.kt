package no.nav.bidrag.person.hendelse.skedulering

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.integrasjon.distribusjon.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import no.nav.bidrag.person.hendelse.prosess.LocalDateTimeTypeAdapter
import no.nav.bidrag.person.hendelse.prosess.LocalDateTypeAdapter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class OverføreHendelser(
    val databasetjeneste: Databasetjeneste,
    val egenskaper: Egenskaper,
    val meldingsprodusent: Meldingsprodusent
) {

    @Scheduled(cron = "\${kjøreplan.overføre_hendelser}")
    @SchedulerLock(name = "hendelser_til_bisys", lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
    fun overføreHendelserTilBisys() {

        // Luke bort hendelser som annulleres før de sendes videre
        databasetjeneste.kansellereIkkeOverførteAnnullerteHendelser()
        var sisteStatusoppdateringFør = LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong())

        var idTilHendelserSomSkalVideresendes = databasetjeneste.henteIdTilHendelserSomSkalOverføresTilBisys(sisteStatusoppdateringFør)

        for (id in idTilHendelserSomSkalVideresendes.iterator()) {
            meldingsprodusent.sendeMelding(egenskaper.wmq.queueNameLivshendelser, oppretteGson().toJson(databasetjeneste.henteMottattHendelse(id)))
        }
    }

    private fun oppretteGson(): Gson {
        var gsonbuilder = GsonBuilder()
        gsonbuilder.registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter().nullSafe())
        gsonbuilder.registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter().nullSafe())
        var gson = gsonbuilder.create()
        return gson
    }
}
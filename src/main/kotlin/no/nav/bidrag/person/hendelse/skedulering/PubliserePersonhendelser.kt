package no.nav.bidrag.person.hendelse.skedulering

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PubliserePersonhendelser {
    @Transactional
    @Scheduled(cron = "\${publisere_personhendelser.kjøreplan}")
    @SchedulerLock(name = "publisere_personhendelser", lockAtLeastFor = "\${publisere_personhendelser.lås.min}", lockAtMostFor = "\${publisere_personhendelser.lås.max}")
    fun publiserePersonhendelser() {
        // Hente identsett for personer som har registrert kontoendring siden forrige sjekk

        // Hente identsett for personer som det er mottatt leesah-meldinger for siden forrige sjekk

        // Publisere melding til intern topic for samtlige personer med endring
    }
}

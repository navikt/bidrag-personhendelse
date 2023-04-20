package no.nav.bidrag.person.hendelse.skedulering

import io.kotest.assertions.assertSoftly
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Hendelsemottak
import no.nav.bidrag.person.hendelse.database.HendelsemottakDao
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles(Testkonfig.PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
open class SletteUtgåtteHendelserTest {

    val personidenter = listOf("12345678901", "1234567890123")

    @Autowired
    lateinit var databasetjeneste: Databasetjeneste

    @Autowired
    lateinit var egenskaper: Egenskaper

    @Autowired
    lateinit var hendelsemottakDao: HendelsemottakDao

    @Autowired
    lateinit var sletteUtgåtteHendelser: SletteUtgåtteHendelser

    @Test
    fun skalSletteKansellerteOgOverførteHendelser() {
        // gitt
        var kansellertHendelse1 = oppretteOgLagreHendelse(
            Status.KANSELLERT,
            LocalDateTime.now().minusDays(egenskaper.generelt.antallDagerLevetidForUtgaatteHendelser.toLong() + 1)
        )
        var kansellertHendelse2 = oppretteOgLagreHendelse(
            Status.KANSELLERT,
            LocalDateTime.now().minusDays(egenskaper.generelt.antallDagerLevetidForUtgaatteHendelser.toLong() + 2)
        )
        var kansellertHendelseUtenforSlettevindu = oppretteOgLagreHendelse(
            Status.KANSELLERT,
            LocalDateTime.now().minusDays(egenskaper.generelt.antallDagerLevetidForUtgaatteHendelser.toLong() - 2)
        )
        var mottattHendelseInnenforSlettevindu = oppretteOgLagreHendelse(
            Status.MOTTATT,
            LocalDateTime.now().minusDays(egenskaper.generelt.antallDagerLevetidForUtgaatteHendelser.toLong() + 2)
        )
        var mottattHendelseUtenforSlettevindu = oppretteOgLagreHendelse(Status.MOTTATT, LocalDateTime.now())
        var overførtHendelse1 = oppretteOgLagreHendelse(
            Status.OVERFØRT,
            LocalDateTime.now().minusDays(egenskaper.generelt.antallDagerLevetidForUtgaatteHendelser.toLong() + 1)
        )
        var overførtHendelse2 = oppretteOgLagreHendelse(
            Status.OVERFØRT,
            LocalDateTime.now().minusDays(egenskaper.generelt.antallDagerLevetidForUtgaatteHendelser.toLong() + 2)
        )
        var overførtHendelseUtenforSlettevindu = oppretteOgLagreHendelse(
            Status.OVERFØRT,
            LocalDateTime.now().minusDays(egenskaper.generelt.antallDagerLevetidForUtgaatteHendelser.toLong() - 2)
        )

        // hvis
        sletteUtgåtteHendelser.sletteUtgåtteHendelserFraDatabase()

        // så
        assertSoftly {
            assertThat(hendelsemottakDao.findById(kansellertHendelse1.id)).isEmpty
            assertThat(hendelsemottakDao.findById(kansellertHendelse2.id)).isEmpty
            assertThat(hendelsemottakDao.findById(kansellertHendelseUtenforSlettevindu.id)).isPresent
            assertThat(hendelsemottakDao.findById(mottattHendelseInnenforSlettevindu.id)).isPresent
            assertThat(hendelsemottakDao.findById(mottattHendelseUtenforSlettevindu.id)).isPresent
            assertThat(hendelsemottakDao.findById(overførtHendelse1.id)).isEmpty
            assertThat(hendelsemottakDao.findById(overførtHendelse2.id)).isEmpty
            assertThat(hendelsemottakDao.findById(overførtHendelseUtenforSlettevindu.id)).isPresent
        }
    }

    private fun oppretteOgLagreHendelse(status: Status, statustidspunkt: LocalDateTime): Hendelsemottak {
        var hendelseid1 = UUID.randomUUID().toString()
        var hendelseMottattUtenforVenteperiode =
            Livshendelse(
                hendelseid1,
                Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
                Livshendelse.Endringstype.OPPRETTET,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now()
            )
        var kansellertHendelseSomSkalSlettes = databasetjeneste.lagreHendelse(hendelseMottattUtenforVenteperiode)
        kansellertHendelseSomSkalSlettes.status = status
        kansellertHendelseSomSkalSlettes.statustidspunkt = statustidspunkt
        return hendelsemottakDao.save(kansellertHendelseSomSkalSlettes)
    }
}

package no.nav.bidrag.person.hendelse.skedulering

import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.HendelsemottakDao
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.exception.OverføringFeiletException
import no.nav.bidrag.person.hendelse.integrasjon.distribusjon.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles(Testkonfig.PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
open class OverføreHendelserTest {

    val personidenter = listOf("12345678901", "1234567890123")

    @Autowired
    lateinit var hendelsemottakDao: HendelsemottakDao
    lateinit var databasetjeneste: Databasetjeneste

    @Autowired
    lateinit var egenskaper: Egenskaper

    @MockK
    lateinit var meldingsprodusent: Meldingsprodusent
    lateinit var overføreHendelser: OverføreHendelser

    @BeforeEach
    fun initialisere() {
        MockKAnnotations.init(this)
        clearAllMocks()
        databasetjeneste = Databasetjeneste(hendelsemottakDao)
        hendelsemottakDao.deleteAll()
        overføreHendelser = OverføreHendelser(databasetjeneste, egenskaper, meldingsprodusent)
        every { meldingsprodusent.sendeMelding(any(), any()) } returns Unit
    }

    @Test
    fun `skal sette status til OVERFØRING_FEILET dersom exception oppstår under sending`(){

        // gitt
        var hendelseid1 = "c096ca6f-9801-4543-9a44-116f4ed806ce"
        var hendelseMottattUtenforVenteperiode =
            Livshendelse(hendelseid1, Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1, Livshendelse.Endringstype.OPPRETTET, personidenter)
        var lagretHendelseVenteperiodeUtløpt = databasetjeneste.lagreHendelse(hendelseMottattUtenforVenteperiode)
        lagretHendelseVenteperiodeUtløpt.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 5)
        var oppdatertHendelseMedUtløptVenteperiode = hendelsemottakDao.save(lagretHendelseVenteperiodeUtløpt)
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseMedUtløptVenteperiode.statustidspunkt)

        var hendelseid2 = "38468520-70f2-40c0-b4ae-6c765c307a7d"
        var hendelseMottattInnenforVenteperiode = Livshendelse(
            hendelseid2,
            Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
            Livshendelse.Endringstype.ANNULLERT,
            personidenter
        )
        var lagretHendelserVenteperiodeIkkeUtløpt = databasetjeneste.lagreHendelse(hendelseMottattInnenforVenteperiode)
        lagretHendelserVenteperiodeIkkeUtløpt.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() -5)
        var oppdatertHendelseVenteperiodeIkkeUtløpt = hendelsemottakDao.save(lagretHendelserVenteperiodeIkkeUtløpt)
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseVenteperiodeIkkeUtløpt.statustidspunkt)

        var hendelseid3 = "87925614-70f2-40c0-b4ae-6c765c308h8h"
        var hendelseMedStatusOverført = Livshendelse(
            hendelseid3,
            Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
            Livshendelse.Endringstype.ANNULLERT,
            personidenter
        )
        var lagretHendelseMedStatusOverført = databasetjeneste.lagreHendelse(hendelseMedStatusOverført)
        var oppdatertHendelseMedStatusOverført = hendelsemottakDao.save(lagretHendelseMedStatusOverført)
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseMedStatusOverført.statustidspunkt)

        every { meldingsprodusent.sendeMelding(any(), any()) } throws OverføringFeiletException("auda!")

        // hvis
        overføreHendelser.overføreHendelserTilBisys()

        // så
        val meldingTilKø = slot<String>()
        verify(exactly = 1) { meldingsprodusent.sendeMelding(egenskaper.wmq.queueNameLivshendelser, capture(meldingTilKø)) }
        assertThat(meldingTilKø.captured).contains(hendelseMottattUtenforVenteperiode.hendelseid)
        assertThat(databasetjeneste.henteHendelse(lagretHendelseVenteperiodeUtløpt.id).get().status).isEqualTo(Status.OVERFØRING_FEILET)
    }

    @Test
    fun skalOverføreHendelserMedStatusMottattOgUtløptVentetid() {

        // gitt
        var hendelseid1 = "c096ca6f-9801-4543-9a44-116f4ed806ce"
        var hendelseMottattUtenforVenteperiode =
            Livshendelse(hendelseid1, Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1, Livshendelse.Endringstype.OPPRETTET, personidenter)
        var lagretHendelseVenteperiodeUtløpt = databasetjeneste.lagreHendelse(hendelseMottattUtenforVenteperiode)
        lagretHendelseVenteperiodeUtløpt.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 5)
        var oppdatertHendelseMedUtløptVenteperiode = hendelsemottakDao.save(lagretHendelseVenteperiodeUtløpt)
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseMedUtløptVenteperiode.statustidspunkt)

        var hendelseid2 = "38468520-70f2-40c0-b4ae-6c765c307a7d"
        var hendelseMottattInnenforVenteperiode = Livshendelse(
            hendelseid2,
            Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
            Livshendelse.Endringstype.ANNULLERT,
            personidenter
        )
        var lagretHendelserVenteperiodeIkkeUtløpt = databasetjeneste.lagreHendelse(hendelseMottattInnenforVenteperiode)
        lagretHendelserVenteperiodeIkkeUtløpt.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() -5)
        var oppdatertHendelseVenteperiodeIkkeUtløpt = hendelsemottakDao.save(lagretHendelserVenteperiodeIkkeUtløpt)
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseVenteperiodeIkkeUtløpt.statustidspunkt)

        var hendelseid3 = "87925614-70f2-40c0-b4ae-6c765c308h8h"
        var hendelseMedStatusOverført = Livshendelse(
            hendelseid3,
            Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
            Livshendelse.Endringstype.ANNULLERT,
            personidenter
        )
        var lagretHendelseMedStatusOverført = databasetjeneste.lagreHendelse(hendelseMedStatusOverført)
        var oppdatertHendelseMedStatusOverført = hendelsemottakDao.save(lagretHendelseMedStatusOverført)
        log.info("Lagret hendelse med statustidspunkt {}", oppdatertHendelseMedStatusOverført.statustidspunkt)

        // hvis
        overføreHendelser.overføreHendelserTilBisys()

        // så
        val meldingTilKø = slot<String>()
        verify(exactly = 1) { meldingsprodusent.sendeMelding(egenskaper.wmq.queueNameLivshendelser, capture(meldingTilKø)) }
        assertThat(meldingTilKø.captured).contains(hendelseMottattUtenforVenteperiode.hendelseid)
    }

    @Test
    fun `skal ikke overføre flere hendelser enn maks antall om gangen`() {

        // gitt
        var hendelseid1 = "c096ca6f-9801-4543-9a44-116f4ed806ce"
        var hendelse1 =
            Livshendelse(hendelseid1, Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1, Livshendelse.Endringstype.OPPRETTET, personidenter)
        var lagretHendelse1 = databasetjeneste.lagreHendelse(hendelse1)
        lagretHendelse1.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 5)
        hendelsemottakDao.save(lagretHendelse1)


        var hendelseid2 = "38468520-70f2-40c0-b4ae-6c765c307a7d"
        var hendelse2 = Livshendelse(
            hendelseid2,
            Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
            Livshendelse.Endringstype.ANNULLERT,
            personidenter
        )
        var lagretHendelse2 = databasetjeneste.lagreHendelse(hendelse2)
        lagretHendelse2.statustidspunkt =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 2)
         hendelsemottakDao.save(lagretHendelse2)

        // hvis
        overføreHendelser.overføreHendelserTilBisys()

        // så
        val meldingTilKø = slot<String>()
        // Maks antall satt i test application.yml (egenskaper.generelt.maksAntallMeldingerSomOverfoeresTilBisysOmGangen)
        verify(exactly = 1) { meldingsprodusent.sendeMelding(egenskaper.wmq.queueNameLivshendelser, capture(meldingTilKø)) }
        assertThat(meldingTilKø.captured).contains(hendelse1.hendelseid)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(OverføreHendelser::class.java)
    }
}
package no.nav.bidrag.person.hendelse.database

import io.kotest.matchers.shouldBe
import jakarta.transaction.Transactional
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Opplysningstype
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig.Companion.PROFIL_TEST
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles


@ActiveProfiles(PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
open class DatabasetjenesteTest {

    val personidenter = listOf("12345678901", "1234567890123")

    @Autowired
    lateinit var hendelsemottakDao: HendelsemottakDao

    @Autowired
    lateinit var databasetjeneste: Databasetjeneste

    @BeforeEach
    fun initialisere() {
        hendelsemottakDao.deleteAll()
    }

    @Test
    @Transactional
    fun skalKansellereTidligereOgNyHendelseVedAnnulleringDersomTidligereHendelseIkkeErOverført() {

        // gitt
        var hendelseidOpprinneligHendelse = "c096ca6f-9801-4543-9a44-116f4ed806ce"
        var opprinneligHendelse =
            Livshendelse(hendelseidOpprinneligHendelse, Opplysningstype.BOSTEDSADRESSE_V1, Endringstype.OPPRETTET, personidenter)
        var lagretOpprinneligHendelse = databasetjeneste.lagreHendelse(opprinneligHendelse)

        var hendelseidAnnulleringshendelse = "38468520-70f2-40c0-b4ae-6c765c307a7d"
        var annulleringAvOpprinneligHendelse = Livshendelse(
            hendelseidAnnulleringshendelse,
            Opplysningstype.BOSTEDSADRESSE_V1,
            Endringstype.ANNULLERT,
            personidenter,
            hendelseidOpprinneligHendelse
        )
        var lagretAnnulleringAvOpprinneligHendelse = databasetjeneste.lagreHendelse(annulleringAvOpprinneligHendelse)

        // hvis
        databasetjeneste.kansellereIkkeOverførteAnnullerteHendelser()

        // så
        var lagretOpprinneligHendelseEtterKansellering = hendelsemottakDao.findById(lagretOpprinneligHendelse.id)
        var lagretNyHendelseEtterKansellering = hendelsemottakDao.findById(lagretAnnulleringAvOpprinneligHendelse.id)

        assertSoftly {
            lagretOpprinneligHendelseEtterKansellering.isPresent
            lagretOpprinneligHendelseEtterKansellering.get().status shouldBe Status.KANSELLERT
            lagretNyHendelseEtterKansellering.isPresent
            lagretNyHendelseEtterKansellering.get().status shouldBe Status.KANSELLERT
        }
    }

    @Test
    @Transactional
    fun tidligereHendelseidFinnesIkkeIDatabasen() {

        // gitt
        var hendelseidOpprinneligHendelse = "c096ca6f-9801-4543-9a44-116f4ed806ce"

        var hendelseidAnnulleringshendelse = "38468520-70f2-40c0-b4ae-6c765c307a7d"
        var annulleringAvOpprinneligHendelse = Livshendelse(
            hendelseidAnnulleringshendelse,
            Opplysningstype.BOSTEDSADRESSE_V1,
            Endringstype.ANNULLERT,
            personidenter,
            hendelseidOpprinneligHendelse
        )
        var lagretAnnulleringAvOpprinneligHendelse = databasetjeneste.lagreHendelse(annulleringAvOpprinneligHendelse)

        // hvis
        databasetjeneste.kansellereIkkeOverførteAnnullerteHendelser()

        // så
        var lagretNyHendelseEtterKansellering = hendelsemottakDao.findById(lagretAnnulleringAvOpprinneligHendelse.id)

        assertSoftly {
            lagretNyHendelseEtterKansellering.isPresent
            lagretNyHendelseEtterKansellering.get().status shouldBe Status.MOTTATT
        }
    }
}
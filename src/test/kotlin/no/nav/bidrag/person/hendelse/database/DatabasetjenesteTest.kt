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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles(PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
class DatabasetjenesteTest {

    private val personidenter = listOf("12345678901", "1234567890123")

    @Autowired
    lateinit var hendelsemottakDao: HendelsemottakDao

    @Autowired
    lateinit var databasetjeneste: Databasetjeneste

    @Nested
    open inner class Hendelsemottak {

        @BeforeEach
        fun initialisere() {
            hendelsemottakDao.deleteAll()
        }

        @Test
        @Transactional
        open fun skalKansellereTidligereOgNyHendelseVedAnnulleringDersomTidligereHendelseIkkeErOverført() {
            // gitt
            var hendelseidOpprinneligHendelse = "c096ca6f-9801-4543-9a44-116f4ed806ce"
            var opprinneligHendelse =
                Livshendelse(
                    hendelseidOpprinneligHendelse,
                    Opplysningstype.BOSTEDSADRESSE_V1,
                    Endringstype.OPPRETTET,
                    personidenter,
                    personidenter.first { it.length == 13 },
                    LocalDateTime.now()
                )
            var lagretOpprinneligHendelse = databasetjeneste.lagreHendelse(opprinneligHendelse)

            var hendelseidAnnulleringshendelse = "38468520-70f2-40c0-b4ae-6c765c307a7d"
            var annulleringAvOpprinneligHendelse = Livshendelse(
                hendelseidAnnulleringshendelse,
                Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.ANNULLERT,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
                hendelseidOpprinneligHendelse
            )

            // hvis
            var lagretAnnulleringAvOpprinneligHendelse =
                databasetjeneste.lagreHendelse(annulleringAvOpprinneligHendelse)

            // så
            var lagretOpprinneligHendelseEtterKansellering = hendelsemottakDao.findById(lagretOpprinneligHendelse.id)
            var lagretNyHendelseEtterKansellering =
                hendelsemottakDao.findById(lagretAnnulleringAvOpprinneligHendelse.id)

            assertSoftly {
                lagretOpprinneligHendelseEtterKansellering.isPresent
                lagretOpprinneligHendelseEtterKansellering.get().status shouldBe Status.KANSELLERT
                lagretNyHendelseEtterKansellering.isPresent
                lagretNyHendelseEtterKansellering.get().status shouldBe Status.KANSELLERT
            }
        }

        @Test
        fun `skal kansellere opphør av bostedsadresse`() {
            // gitt
            var hendelseid = "c096ca6f-9801-4543-9a44-116f4ed806ce"
            var hendelse =
                Livshendelse(
                    hendelseid,
                    Opplysningstype.BOSTEDSADRESSE_V1,
                    Endringstype.OPPHOERT,
                    personidenter,
                    personidenter.first { it.length == 13 },
                    LocalDateTime.now()
                )

            // hvis
            var lagretHendelse = databasetjeneste.lagreHendelse(hendelse)

            // så
            assertSoftly {
                lagretHendelse.status shouldBe Status.KANSELLERT
            }
        }

        @Test
        @Transactional
        open fun tidligereHendelseidFinnesIkkeIDatabasen() {
            // gitt
            var hendelseidOpprinneligHendelse = "c096ca6f-9801-4543-9a44-116f4ed806ce"

            var hendelseidAnnulleringshendelse = "38468520-70f2-40c0-b4ae-6c765c307a7d"
            var annulleringAvOpprinneligHendelse = Livshendelse(
                hendelseidAnnulleringshendelse,
                Opplysningstype.BOSTEDSADRESSE_V1,
                Endringstype.ANNULLERT,
                personidenter,
                personidenter.first { it.length == 13 },
                LocalDateTime.now(),
                hendelseidOpprinneligHendelse
            )

            // hvis
            var lagretAnnulleringAvOpprinneligHendelse =
                databasetjeneste.lagreHendelse(annulleringAvOpprinneligHendelse)

            // så
            var lagretNyHendelseEtterKansellering =
                hendelsemottakDao.findById(lagretAnnulleringAvOpprinneligHendelse.id)

            assertSoftly {
                lagretNyHendelseEtterKansellering.isPresent
                lagretNyHendelseEtterKansellering.get().status shouldBe Status.MOTTATT
            }
        }
    }

    private fun tidspunkterErInnenforVindu(start: LocalDateTime, tidspunkt: LocalDateTime): Boolean {
        return tidspunkt.isAfter(start) && tidspunkt.isBefore(LocalDateTime.now().plusSeconds(2))
    }
}

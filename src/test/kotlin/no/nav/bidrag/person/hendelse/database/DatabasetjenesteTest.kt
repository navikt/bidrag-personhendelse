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
    lateinit var kontoendringDao: KontoendringDao

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
                    LocalDateTime.now(),
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
                hendelseidOpprinneligHendelse)

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
                    personidenter.first{it.length == 13},
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

    @Nested
    inner class Kontoendring {

        @BeforeEach
        fun initialisere() {
            kontoendringDao.deleteAll()
        }

        @Test
        fun `lagre kontoendring for ny kontoeier`() {

            // gitt
            var kontoeier = "123456"
            var tidspunktFørLagring = LocalDateTime.now()

            // hvis
            var kontoendring = databasetjeneste.lagreKontoendring(kontoeier)

            // så
            var kontoendringFraDatabase = kontoendringDao.findById(kontoendring.id)

            assertSoftly {
                kontoendringFraDatabase.isPresent
                kontoendringFraDatabase.get().status shouldBe StatusKontoendring.MOTTATT
                kontoendringFraDatabase.get().kontoeier shouldBe kontoeier
                tidspunkterErInnenforVindu(tidspunktFørLagring, kontoendringFraDatabase.get().mottatt)
                tidspunkterErInnenforVindu(tidspunktFørLagring, kontoendringFraDatabase.get().statustidspunkt)
                kontoendringFraDatabase.get().publisert shouldBe null
            }
        }

        @Test
        fun `lagre kontoendring for kontoeier med eksisterende mottatt-innslag i databasen`() {

            // gitt
            var kontoeier = "123456"
            var tidspunktFørLagring = LocalDateTime.now()
            var tidligereMottattKontoendring = databasetjeneste.lagreKontoendring(kontoeier)

            // hvis
            var nyttKontoendringsinnslag = databasetjeneste.lagreKontoendring(kontoeier)

            // så
            var forrigeLagredeKontoendringsinnslag = kontoendringDao.findById(tidligereMottattKontoendring.id)
            var nyttLagretKontoendringsinnslag = kontoendringDao.findById(nyttKontoendringsinnslag.id)

            assertSoftly {
                forrigeLagredeKontoendringsinnslag.isPresent
                nyttLagretKontoendringsinnslag.isPresent
                forrigeLagredeKontoendringsinnslag.get().status shouldBe StatusKontoendring.TRUKKET
                nyttLagretKontoendringsinnslag.get().status shouldBe StatusKontoendring.MOTTATT
                forrigeLagredeKontoendringsinnslag.get().kontoeier shouldBe kontoeier
                nyttLagretKontoendringsinnslag.get().kontoeier shouldBe kontoeier
                forrigeLagredeKontoendringsinnslag.get().mottatt.isBefore(nyttLagretKontoendringsinnslag.get().mottatt)
                tidspunkterErInnenforVindu(tidspunktFørLagring, nyttLagretKontoendringsinnslag.get().mottatt)
                tidspunkterErInnenforVindu(
                    tidspunktFørLagring,
                    forrigeLagredeKontoendringsinnslag.get().statustidspunkt
                )
                tidspunkterErInnenforVindu(tidspunktFørLagring, nyttLagretKontoendringsinnslag.get().statustidspunkt)
                forrigeLagredeKontoendringsinnslag.get().publisert shouldBe null
                nyttLagretKontoendringsinnslag.get().publisert shouldBe null
            }
        }
    }

    private fun tidspunkterErInnenforVindu(start: LocalDateTime, tidspunkt: LocalDateTime): Boolean {
        return tidspunkt.isAfter(start) && tidspunkt.isBefore(LocalDateTime.now().plusSeconds(2))
    }
}

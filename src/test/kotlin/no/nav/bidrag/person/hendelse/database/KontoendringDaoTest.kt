package no.nav.bidrag.person.hendelse.database

import io.kotest.matchers.shouldBe
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest(
    classes = [Teststarter::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KontoendringDaoTest {

    @Autowired
    lateinit var egenskaper: Egenskaper

    @Autowired
    lateinit var kontoendringDao: KontoendringDao

    @Autowired
    lateinit var aktorDao: AktorDao

    @BeforeEach
    fun initialisere() {
        kontoendringDao.deleteAll()
    }

    @Test
    fun skalLagreNyKontoendring() {
        // gitt
        var aktør: Aktor = aktorDao.save(Aktor("1231234567891"))
        var nyKontoendring = Kontoendring(aktør, aktør.aktorid)

        // hvis
        var lagretKontoendring = kontoendringDao.save(nyKontoendring)

        // så
        var eksisterer = kontoendringDao.findById(lagretKontoendring.id)
        assertThat(eksisterer).isPresent
    }

    @Test
    fun skalHenteKontoendringSomErKlarForPublisering() {
        // gitt
        val aktør: Aktor = aktorDao.save(Aktor("1231234567891", LocalDateTime.now().minusDays(1)))
        kontoendringDao.save(Kontoendring(aktør, aktør.aktorid, LocalDateTime.now().minusDays(1)))

        // hvis
        val kontoeiere = kontoendringDao.henteAktørerForPublisering(
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusHours(egenskaper.generelt.antallTimerSidenForrigePublisering.toLong())
        )

        // så
        assertSoftly {
            kontoeiere.size shouldBe 1
            kontoeiere.first().aktorid shouldBe aktør.aktorid
        }
    }
}

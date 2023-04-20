package no.nav.bidrag.person.hendelse.database

import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [Teststarter::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
 class HendelsemottakDaoTest {

    @Autowired
    lateinit var hendelsemottakDao: HendelsemottakDao

    @Test
    fun skalLagreHendelse() {

        // gitt
        var hendelseid = "123"
        var opplysningstype = Livshendelse.Opplysningstype.SIVILSTAND_V1
        var hendelsemottak = Hendelsemottak(hendelseid, opplysningstype)

        // hvis
        hendelsemottakDao.save(hendelsemottak)

        // så
        var eksisterer = hendelsemottakDao.existsByHendelseidAndOpplysningstype(hendelseid, opplysningstype)
        assertThat(eksisterer).isTrue
    }
}
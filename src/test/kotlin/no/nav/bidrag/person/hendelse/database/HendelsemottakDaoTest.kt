package no.nav.bidrag.person.hendelse.database

import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.domene.Livshendelse
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

    @Autowired
    lateinit var aktorDao: AktorDao

    @Test
    fun skalLagreHendelse() {
        // gitt
        var personidenter = listOf("12345678910", "1234567891013")
        var aktørid = personidenter.first { it.length == 13 }
        var aktør = aktorDao.save(Aktor(aktørid))

        var hendelseid = "123"
        var opplysningstype = Livshendelse.Opplysningstype.SIVILSTAND_V1
        var endringstype = Livshendelse.Endringstype.OPPRETTET

        var hendelsemottak = Hendelsemottak(hendelseid, opplysningstype, endringstype, personidenter.toString(), aktør)

        // hvis
        hendelsemottakDao.save(hendelsemottak)

        // så
        var eksisterer = hendelsemottakDao.existsByHendelseidAndOpplysningstype(hendelseid, opplysningstype)
        assertThat(eksisterer).isTrue
    }
}

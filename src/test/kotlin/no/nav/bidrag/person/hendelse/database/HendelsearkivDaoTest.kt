package no.nav.bidrag.person.hendelse.database

import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
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
open class HendelsearkivDaoTest {

    @Autowired
    lateinit var hendelsearkivDao: HendelsearkivDao

    @Test
    fun skalArkivereHendelse() {

        // gitt
        var hendelseid = "123"
        var opplysningstype = Livshendelsebehandler.Opplysningstype.SIVILSTAND_V1
        var hendelsearkiv = Hendelsearkiv(hendelseid, opplysningstype.toString())

        // hvis
        hendelsearkivDao.save(hendelsearkiv)

        // så
        var eksisterer = hendelsearkivDao.existsByHendelseidAndOpplysningstype(hendelseid, opplysningstype.toString())
        assertThat(eksisterer).isTrue()
    }
}
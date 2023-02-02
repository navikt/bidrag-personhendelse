package no.nav.bidrag.person.hendelse.konsumere

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.domene.*
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Opplysningstype
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LivshendelsebehandlerTest {
    lateinit var mockDatabasetjeneste: Databasetjeneste
    lateinit var service: Livshendelsebehandler

    val personidenter = listOf("12345678901", "1234567890123")

    @BeforeEach
    internal fun oppsett() {
        mockDatabasetjeneste = mockk(relaxed = true)
        service = Livshendelsebehandler(mockDatabasetjeneste)
        clearAllMocks()
    }

    @Test
    fun `Skal prosessere dødsfallhendelse`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = oppretteLivshendelseForDødsfall(hendelseId, Opplysningstype.DOEDSFALL_V1, Endringstype.OPPRETTET, LocalDate.now())
        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal prosessere utflyttingshendelse`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse =
            oppretteLivshendelseForUtflytting(
                hendelseId, Opplysningstype.UTFLYTTING_FRA_NORGE, Endringstype.OPPRETTET, Utflytting("SWE", null, LocalDate.now())
            )

        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal prosessere sivilstandhendelse`() {
        val hendelseId = UUID.randomUUID().toString()

        val livshendelse =
            oppretteLivshendelseForSivilstand(hendelseId, Opplysningstype.SIVILSTAND_V1, Endringstype.OPPRETTET, Sivilstand("GIFT"))

        service.prosesserNyHendelse(livshendelse)
        service.prosesserNyHendelse(livshendelse.copy(sivilstand = Sivilstand("UOPPGITT")))
    }

    @Test
    fun `Skal prosessere fødselsmelding`() {
        val hendelseId = UUID.randomUUID().toString()

        val livshendelse =
            oppretteLivshendelseForFødsel(hendelseId, Opplysningstype.FOEDSEL_V1, Endringstype.OPPRETTET, Fødsel("NOR", LocalDate.now()))

        service.prosesserNyHendelse(livshendelse)

        verify(exactly = 1) {
            mockDatabasetjeneste.lagreHendelse(livshendelse)
        }
    }

    @Test
    fun `Skal ignorere fødselshendelser utenfor norge`() {
        val hendelseId = UUID.randomUUID().toString()

        val livshendelse =
            oppretteLivshendelseForFødsel(hendelseId, Opplysningstype.FOEDSEL_V1, Endringstype.OPPRETTET, Fødsel("POL", LocalDate.now()))

        service.prosesserNyHendelse(livshendelse)
        verify(exactly = 0) { mockDatabasetjeneste.lagreHendelse(livshendelse) }
        service.prosesserNyHendelse(livshendelse.copy(fødsel = Fødsel("NOR")))
        verify(exactly = 1) { mockDatabasetjeneste.lagreHendelse(livshendelse) }
        service.prosesserNyHendelse(livshendelse.copy(fødsel = Fødsel(null)))
        verify(exactly = 2) { mockDatabasetjeneste.lagreHendelse(livshendelse) }
    }

    fun oppretteLivshendelseForFødsel(
        hendelseId: String,
        opplysningstype: Opplysningstype,
        endringstype: Endringstype,
        fødsel: Fødsel
    ): Livshendelse {
        return Livshendelse(
            hendelseId, opplysningstype, endringstype, personidenter, null, null, null, null, fødsel
        )
    }

    fun oppretteLivshendelseForDødsfall(
        hendelseId: String,
        opplysningstype: Opplysningstype,
        endringstype: Endringstype,
        dødsdato: LocalDate
    ): Livshendelse {
        return Livshendelse(hendelseId, opplysningstype, endringstype, personidenter, null, dødsdato)
    }

    fun oppretteLivshendelseForUtflytting(
        hendelseId: String,
        opplysningstype: Opplysningstype,
        endringstype: Endringstype,
        utflytting: Utflytting
    ): Livshendelse {
        return Livshendelse(
            hendelseId, opplysningstype, endringstype, personidenter, null, null, null,
            null, null, null, null, utflytting
        )
    }

    fun oppretteLivshendelseForSivilstand(
        hendelseId: String,
        opplysningstype: Opplysningstype,
        endringstype: Endringstype,
        sivilstand: Sivilstand
    ): Livshendelse {
        return Livshendelse(
            hendelseId, opplysningstype, endringstype, personidenter, null, null, null,
            null, null, null, null, null, sivilstand
        )
    }
}

package no.nav.bidrag.person.hendelse.konsumere

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.person.hendelse.domene.Fødsel
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.domene.Sivilstand
import no.nav.bidrag.person.hendelse.domene.Utflytting
import no.nav.bidrag.person.hendelse.database.HendelsearkivDao
import no.nav.bidrag.person.hendelse.integrasjon.distribuere.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Wmq
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.random.Random
import kotlin.random.nextUInt

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LivshendelsebehandlerTest {
    lateinit var mockMeldingsprodusent: Meldingsprodusent
    lateinit var mockHendelsearkivDao: HendelsearkivDao
    lateinit var service: Livshendelsebehandler
    lateinit var egenskaperWmq: Wmq

    @BeforeEach
    internal fun oppsett() {
        mockMeldingsprodusent = mockk(relaxed = true)
        egenskaperWmq = mockk(relaxed = true)
        mockHendelsearkivDao = mockk(relaxed = true)
        service = Livshendelsebehandler(egenskaperWmq, mockHendelsearkivDao, mockMeldingsprodusent)
        clearAllMocks()
    }

    @Test
    fun `Skal prosessere dødsfallhendelse`() {
        val hendelseId = UUID.randomUUID().toString()

        val livshendelse = Livshendelse(
            hendelseId,
            Random.nextUInt().toLong(),
            LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toString(),
            "PDL",
            Livshendelsebehandler.Opplysningstype.DOEDSFALL_V1.toString(),
            Livshendelsebehandler.OPPRETTET,
            listOf("12345678901", "1234567890123"),
            LocalDate.now()
        )

        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal prosessere utflyttingshendelse`() {
        val hendelseId = UUID.randomUUID().toString()

        val livshendelse = Livshendelse(
            hendelseId,
            Random.nextUInt().toLong(),
            LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toString(),
            "PDL",
            Livshendelsebehandler.Opplysningstype.SIVILSTAND_V1.toString(),
            Livshendelsebehandler.OPPRETTET,
            listOf("12345678901", "1234567890123"),
            null,
            null,
            null,
            null,
            null,
            null,
            Utflytting("SWE", null,  LocalDate.now())
        )

        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal prosessere sivilstandhendelse`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse(
            hendelseId,
            Random.nextUInt().toLong(),
            LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toString(),
            "PDL",
            Livshendelsebehandler.Opplysningstype.SIVILSTAND_V1.toString(),
            Livshendelsebehandler.OPPRETTET,
            listOf("12345678901", "1234567890123"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Sivilstand("GIFT")
        )

        service.prosesserNyHendelse(livshendelse)
        service.prosesserNyHendelse(livshendelse.copy(sivilstand = Sivilstand("UOPPGITT")))
    }

    @Test
    fun `Skal prosessere fødselsmelding`() {
        val hendelseId = UUID.randomUUID().toString()

        val livshendelse = Livshendelse(
            hendelseId,
            Random.nextUInt().toLong(),
            LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toString(),
            "PDL",
            Livshendelsebehandler.Opplysningstype.FOEDSEL_V1.toString(),
            Livshendelsebehandler.OPPRETTET,
            listOf("12345678901", "1234567890123"),
            null,
            null,
            null,
            Fødsel("NOR", LocalDate.now())
        )

        service.prosesserNyHendelse(livshendelse)

        verify(exactly = 1) {
            mockMeldingsprodusent.sendeMelding(any(), any())
        }
    }

    @Test
    fun `Skal ignorere fødselshendelser utenfor norge`() {
        val hendelseId = UUID.randomUUID().toString()

        val livshendelse = Livshendelse(
            hendelseId,
            Random.nextUInt().toLong(),
            LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toString(),
            "PDL",
            Livshendelsebehandler.Opplysningstype.FOEDSEL_V1.toString(),
            Livshendelsebehandler.OPPRETTET,
            listOf("12345678901", "1234567890123"),
            null,
            null,
            null,
            Fødsel("POL", LocalDate.now())
        )

        service.prosesserNyHendelse(livshendelse)
        verify(exactly = 0) { mockMeldingsprodusent.sendeMelding(any(), any()) }
        service.prosesserNyHendelse(livshendelse.copy(fødsel = Fødsel("NOR")))
        verify(exactly = 1) { mockMeldingsprodusent.sendeMelding(any(), any()) }
        service.prosesserNyHendelse(livshendelse.copy(fødsel = Fødsel(null)))
        verify(exactly = 2) { mockMeldingsprodusent.sendeMelding(any(), any()) }
    }
}

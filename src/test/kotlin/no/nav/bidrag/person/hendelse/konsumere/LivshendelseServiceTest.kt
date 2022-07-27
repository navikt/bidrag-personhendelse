package no.nav.bidrag.person.hendelse.konsumere

import io.mockk.clearAllMocks
import io.mockk.mockk
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.integrasjon.motta.LivshendelseService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.core.env.Environment
import java.time.LocalDate
import java.util.*
import kotlin.random.Random
import kotlin.random.nextUInt

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LivshendelseServiceTest {
    lateinit var mockenv: Environment
    lateinit var service: LivshendelseService

    @BeforeEach
    internal fun setUp() {
        mockenv = mockk<Environment>(relaxed = true)
        service = LivshendelseService()
        clearAllMocks()
    }

    @Test
    fun `Skal opprette VurderLivshendelseTask for dødsfallhendelse`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse(
            offset = Random.nextUInt().toLong(),
            gjeldendeAktørId = "1234567890123",
            hendelseId = hendelseId,
            personIdenter = listOf("12345678901", "1234567890123"),
            endringstype = LivshendelseService.OPPRETTET,
            opplysningstype = LivshendelseService.OPPLYSNINGSTYPE_DØDSFALL,
            dødsdato = LocalDate.now()
        )

        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal opprette VurderLivshendelseTask for utflyttingshendelse`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse(
            offset = Random.nextUInt().toLong(),
            gjeldendeAktørId = "1234567890123",
            hendelseId = hendelseId,
            personIdenter = listOf("12345678901", "1234567890123"),
            endringstype = LivshendelseService.OPPRETTET,
            opplysningstype = LivshendelseService.OPPLYSNINGSTYPE_UTFLYTTING,
            utflyttingsdato = LocalDate.now()
        )

        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal opprette VurderLivshendelseTask for sivilstandhendelse GIFT`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse(
            offset = Random.nextUInt().toLong(),
            gjeldendeAktørId = "1234567890123",
            hendelseId = hendelseId,
            personIdenter = listOf("12345678901", "1234567890123"),
            endringstype = LivshendelseService.OPPRETTET,
            opplysningstype = LivshendelseService.OPPLYSNINGSTYPE_SIVILSTAND,
            sivilstand = "GIFT",
            sivilstandDato = LocalDate.of(2022, 2, 22),
        )

        service.prosesserNyHendelse(livshendelse)
        service.prosesserNyHendelse(livshendelse.copy(sivilstand = "UOPPGITT"))
    }

    @Test
    fun `Skal opprette MottaFødselshendelseTask med fnr på payload`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse(
            offset = Random.nextUInt().toLong(),
            gjeldendeAktørId = "1234567890123",
            hendelseId = hendelseId,
            personIdenter = listOf("12345678901", "1234567890123"),
            endringstype = LivshendelseService.OPPRETTET,
            opplysningstype = LivshendelseService.OPPLYSNINGSTYPE_FØDSEL,
            fødselsdato = LocalDate.now(),
            fødeland = "NOR"
        )

        service.prosesserNyHendelse(livshendelse)
    }

    @Test
    fun `Skal ignorere fødselshendelser utenfor norge`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse(
            offset = Random.nextUInt().toLong(),
            gjeldendeAktørId = "1234567890123",
            hendelseId = hendelseId,
            personIdenter = listOf("12345678901", "1234567890123"),
            endringstype = LivshendelseService.OPPRETTET,
            opplysningstype = LivshendelseService.OPPLYSNINGSTYPE_FØDSEL,
            fødselsdato = LocalDate.now(),
            fødeland = "POL"
        )

        service.prosesserNyHendelse(livshendelse)
        service.prosesserNyHendelse(livshendelse.copy(fødeland = "NOR"))
        service.prosesserNyHendelse(livshendelse.copy(fødeland = null))
    }

    @Test
    fun `Skal opprette MottaAnnullerFødselTask når endringstype er ANNULLERT`() {
        val hendelseId = UUID.randomUUID().toString()
        val livshendelse = Livshendelse(
            offset = Random.nextUInt().toLong(),
            gjeldendeAktørId = "1234567890123",
            hendelseId = hendelseId,
            personIdenter = listOf("12345678901", "1234567890123"),
            endringstype = LivshendelseService.ANNULLERT,
            opplysningstype = LivshendelseService.OPPLYSNINGSTYPE_FØDSEL,
            fødselsdato = LocalDate.now(),
            fødeland = "NOR",
            tidligereHendelseId = "unknown"
        )

        service.prosesserNyHendelse(livshendelse)
    }
}

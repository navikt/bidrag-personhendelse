package no.nav.bidrag.person.hendelse.integrasjon.mottak

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Opplysningstype
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.bostedsadresse.Bostedsadresse
import no.nav.person.pdl.leesah.common.adresse.Vegadresse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.foedsel.Foedsel
import no.nav.person.pdl.leesah.folkeregisteridentifikator.Folkeregisteridentifikator
import no.nav.person.pdl.leesah.innflytting.InnflyttingTilNorge
import no.nav.person.pdl.leesah.navn.Navn
import no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockKExtension::class)
class LivshendelsemottakTest {

    @MockK
    lateinit var livshendelsebehandler: Livshendelsebehandler
    lateinit var livshendelsemottak: Livshendelsemottak

    @BeforeEach
    internal fun oppsett() {
        MockKAnnotations.init(this)
        clearAllMocks()
        livshendelsemottak = Livshendelsemottak((livshendelsebehandler))
        every { livshendelsebehandler.prosesserNyHendelse(any()) } returns Unit
    }

    @Test
    fun `skal avbryte prossesering av melding med opplysningstype som ikke støttes`() {
        // gitt
        var personhendelse = henteIkkeStøttetOpplysingstype()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 0) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
    }

    @Test
    fun `skal håndtere dødsfall`() {
        // gitt
        var personhendelse = hentePersonhendelseForDødsfall()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.DOEDSFALL_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.doedsdato).isEqualTo(personhendelse.doedsfall.doedsdato)
    }

    @Test
    fun `skal håndtere bostedsadresse`() {
        // gitt
        var personhendelse = hentePersonhendelseForBostedsadresse()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.BOSTEDSADRESSE_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.flyttedato).isEqualTo(personhendelse.bostedsadresse.angittFlyttedato)
    }

    @Test
    fun `skal håndtere folkeregisteridentifikator`() {
        // gitt
        var personhendelse = hentePersonhendelseForFolkeregisteridentifikator()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }

        assertThat(livshendelseSomSendesTilBehandling.captured).isNotNull
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.FOLKEREGISTERIDENTIFIKATOR_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.folkeregisteridentifikator?.identifikasjonsnummer).isEqualTo(personhendelse.folkeregisteridentifikator.identifikasjonsnummer)
        assertThat(livshendelseSomSendesTilBehandling.captured.folkeregisteridentifikator?.type).isEqualTo(personhendelse.folkeregisteridentifikator.type)
        assertThat(livshendelseSomSendesTilBehandling.captured.folkeregisteridentifikator?.status).isEqualTo(personhendelse.folkeregisteridentifikator.status)
    }

    @Test
    fun `skal håndtere fødsel`() {
        // gitt
        var personhendelse = hentePersonhendelseForFødsel()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.FOEDSEL_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.foedsel?.foedeland).isEqualTo(personhendelse.foedsel.foedeland)
        assertThat(livshendelseSomSendesTilBehandling.captured.foedsel?.foedselsdato).isEqualTo(personhendelse.foedsel.foedselsdato)
    }

    @Test
    fun `skal håndtere innflytting`() {
        // gitt
        var personhendelse = hentePersonhendelseForInnflytting()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.INNFLYTTING_TIL_NORGE)
        assertThat(livshendelseSomSendesTilBehandling.captured.innflytting?.fraflyttingsland).isEqualTo(personhendelse.innflyttingTilNorge.fraflyttingsland)
    }

    @Test
    fun `skal håndtere navn`() {
        // gitt
        var personhendelse = hentePersonhendelseForNavn()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.NAVN_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.navn?.fornavn).isEqualTo(personhendelse.navn.fornavn)
    }

    @Test
    fun `skal håndtere utflytting`() {
        // gitt
        var personhendelse = hentePersonhendelseForUtflytting()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.UTFLYTTING_FRA_NORGE)
        assertThat(livshendelseSomSendesTilBehandling.captured.utflytting?.tilflyttingsland).isEqualTo(personhendelse.utflyttingFraNorge.tilflyttingsland)
        assertThat(livshendelseSomSendesTilBehandling.captured.utflytting?.utflyttingsdato).isEqualTo(personhendelse.utflyttingFraNorge.utflyttingsdato)
    }

    @Test
    fun `skal håndtere sivilstand`() {
        // gitt
        var personhendelse = hentePersonhendelseForSivilstand()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.SIVILSTAND_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.sivilstand?.bekreftelsesdato).isEqualTo(personhendelse.sivilstand.bekreftelsesdato)
        assertThat(livshendelseSomSendesTilBehandling.captured.sivilstand?.sivilstand).isEqualTo(personhendelse.sivilstand.type)
    }

    @Test
    fun `skal håndtere korrigering av sivilstandendring`() {
        // gitt
        var personhendelse = hentePersonhendelseForSivilstandKorrigering()

        var cr = ConsumerRecord(
            "pdl.leesah-v1", 1, 229055,
            Instant.now().toEpochMilli(), TimestampType.CREATE_TIME, 0, 0, "2541031559331",
            personhendelse, RecordHeaders(), Optional.of(0)
        )

        // hvis
        livshendelsemottak.listen(personhendelse, cr)

        // så
        val livshendelseSomSendesTilBehandling = slot<Livshendelse>()
        verify(exactly = 1) { livshendelsebehandler.prosesserNyHendelse(capture(livshendelseSomSendesTilBehandling)) }
        assertThat(livshendelseSomSendesTilBehandling.captured.opplysningstype).isEqualTo(Opplysningstype.SIVILSTAND_V1)
        assertThat(livshendelseSomSendesTilBehandling.captured.sivilstand?.bekreftelsesdato).isEqualTo(personhendelse.sivilstand.bekreftelsesdato)
        assertThat(livshendelseSomSendesTilBehandling.captured.sivilstand?.sivilstand).isEqualTo(personhendelse.sivilstand.type)
        assertThat(livshendelseSomSendesTilBehandling.captured.tidligereHendelseid).isNotEmpty()
        assertThat(livshendelseSomSendesTilBehandling.captured.tidligereHendelseid).isEqualTo(personhendelse.tidligereHendelseId)
    }

    companion object {
        fun henteIkkeStøttetOpplysingstype(): Personhendelse {
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.doedsfall = Doedsfall(LocalDate.now())
            personhendelse.opplysningstype = "TELEFONNUMMER_V1"

            return personhendelse
        }
        fun hentePersonhendelseForDødsfall(): Personhendelse {
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.doedsfall = Doedsfall(LocalDate.now())
            personhendelse.opplysningstype = Opplysningstype.DOEDSFALL_V1.name

            return personhendelse
        }

        fun hentePersonhendelseForBostedsadresse(): Personhendelse {
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.bostedsadresse = Bostedsadresse.newBuilder()
                .setAngittFlyttedato(LocalDate.now())
                .setVegadresse(Vegadresse.newBuilder().setAdressenavn("Korketrekkeren 20").build())
                .build()
            personhendelse.opplysningstype = Opplysningstype.BOSTEDSADRESSE_V1.name

            return personhendelse
        }

        fun hentePersonhendelseForFolkeregisteridentifikator(): Personhendelse {
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.folkeregisteridentifikator = Folkeregisteridentifikator.newBuilder()
                .setIdentifikasjonsnummer("12345678910")
                .setStatus("I_BRUK")
                .setType("FNR")
                .build()
            personhendelse.opplysningstype = Opplysningstype.FOLKEREGISTERIDENTIFIKATOR_V1.name

            return personhendelse
        }

        fun hentePersonhendelseForFødsel(): Personhendelse {
            var fødsel = Foedsel()
            fødsel.foedeland = "NOR"
            fødsel.foedselsdato = LocalDate.now()
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.foedsel = fødsel
            personhendelse.opplysningstype = Opplysningstype.FOEDSEL_V1.name

            return personhendelse
        }

        fun hentePersonhendelseForInnflytting(): Personhendelse {
            var innflytting = InnflyttingTilNorge("POL", "Birk")
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.innflyttingTilNorge = innflytting
            personhendelse.opplysningstype = Opplysningstype.INNFLYTTING_TIL_NORGE.name

            return personhendelse
        }

        fun hentePersonhendelseForNavn(): Personhendelse {
            var navn = Navn.newBuilder().setFornavn("Stolpe").setEtternavn("Hekk").build()
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.navn = navn
            personhendelse.opplysningstype = Opplysningstype.NAVN_V1.name
            return personhendelse
        }

        fun hentePersonhendelseForUtflytting(): Personhendelse {
            var utflytting = UtflyttingFraNorge.newBuilder().setTilflyttingsland("POL").setUtflyttingsdato(LocalDate.now()).build()
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.utflyttingFraNorge = utflytting
            personhendelse.opplysningstype = Opplysningstype.UTFLYTTING_FRA_NORGE.name
            return personhendelse
        }

        fun hentePersonhendelseForSivilstand(): Personhendelse {
            var sivilstand = no.nav.person.pdl.leesah.sivilstand.Sivilstand.newBuilder().setBekreftelsesdato(LocalDate.now()).setType("GIFT").build()
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.sivilstand = sivilstand
            personhendelse.opplysningstype = Opplysningstype.SIVILSTAND_V1.name
            return personhendelse
        }

        fun hentePersonhendelseForSivilstandKorrigering(): Personhendelse {
            var sivilstand = no.nav.person.pdl.leesah.sivilstand.Sivilstand.newBuilder().setBekreftelsesdato(LocalDate.now()).setType("GIFT").build()
            var personhendelse = henteMetadataTilPersonhendelse()
            personhendelse.sivilstand = sivilstand
            personhendelse.tidligereHendelseId = "123"
            personhendelse.endringstype = Endringstype.KORRIGERT
            personhendelse.opplysningstype = Opplysningstype.SIVILSTAND_V1.name
            return personhendelse
        }

        internal fun henteMetadataTilPersonhendelse(): Personhendelse {
            var personidenter = listOf("2541031559331", "07486302423")
            var personhendelse = Personhendelse.newBuilder()
                .setHendelseId("567f35f1-b5c0-4457-8848-01d897d78bba")
                .setPersonidenter(personidenter)
                .setMaster("FREG")
                .setOpprettet(Instant.now())
                .setOpplysningstype("Ikke satt")
                .setEndringstype(Endringstype.OPPRETTET)
                .build()
            return personhendelse
        }
    }
}

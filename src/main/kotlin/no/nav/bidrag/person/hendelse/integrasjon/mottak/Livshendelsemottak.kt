package no.nav.bidrag.person.hendelse.integrasjon.mottak

import no.nav.bidrag.person.hendelse.domene.*
import no.nav.bidrag.person.hendelse.exception.HendelsemottakException
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.bostedsadresse.Bostedsadresse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Collectors

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class Livshendelsemottak(val livshendelsebehandler: Livshendelsebehandler) {

    object MdcKonstanter {
        const val MDC_KALLID = "id-kall"
    }

    @KafkaListener(
        groupId = "leesah-v1.bidrag",
        topics = ["pdl.leesah-v1"],
        id = "bidrag-person-hendelse.leesah-v1",
        idIsGroup = false,
        containerFactory = "kafkaLeesahListenerContainerFactory"
    )
    fun listen(@Payload personhendelse: Personhendelse, cr: ConsumerRecord<String, Personhendelse>) {
        log.info("Livshendelse med hendelseid {} mottatt.", personhendelse.hendelseId)
        SECURE_LOGGER.info("Har mottatt leesah-hendelse $cr")

        var opplysningstype = konvertereOpplysningstype(personhendelse.opplysningstype)

        if (Livshendelse.Opplysningstype.IKKE_STØTTET.equals(opplysningstype)) {
            log.info("Mottok opplysningstype som ikke støttes av løsningen - avbryter videre prosessering.")
            return
        }

        val livshendelse = Livshendelse(
            personhendelse.hendelseId.toString(),
            opplysningstype,
            konvertereEndringstype(personhendelse.endringstype),
            personhendelse.personidenter?.stream()?.map(CharSequence::toString)?.collect(Collectors.toList()),
            personhendelse.tidligereHendelseId?.toString(),
            henteDødsdato(personhendelse.doedsfall),
            henteFlyttedato(personhendelse.bostedsadresse),
            henteFolkeregisteridentifikator(personhendelse.folkeregisteridentifikator),
            henteFødsel(personhendelse.foedsel),
            henteInnflytting(personhendelse.innflyttingTilNorge),
            henteNavn(personhendelse.navn),
            henteUtflytting(personhendelse.utflyttingFraNorge),
            henteSivilstand(personhendelse.sivilstand),
            henteVerge(personhendelse.vergemaalEllerFremtidsfullmakt),
            henteAdressebeskyttelse(personhendelse.adressebeskyttelse),
            cr.offset(),
            LocalDateTime.ofInstant(personhendelse.opprettet, ZoneId.systemDefault()),
            personhendelse.master.toString()
        )

        try {
            MDC.put(MdcKonstanter.MDC_KALLID, livshendelse.hendelseid)
            livshendelsebehandler.prosesserNyHendelse(livshendelse)
        } catch (e: RuntimeException) {
            SECURE_LOGGER.error("Feil i prosessering av leesah-hendelse", e)
            throw RuntimeException("Feil i prosessering av leesah-hendelse")
        } finally {
            MDC.clear()
        }
    }

    private fun konvertereOpplysningstype(pdlOpplysningstype: CharSequence?): Livshendelse.Opplysningstype {
        return if (pdlOpplysningstype == null) {
            log.error("Opplysningstype i mottatt melding var null. Avbryter prosessering.")
            throw HendelsemottakException("Opplysningstype i mottatt melding var null!");
        } else {
            try {
                return Livshendelse.Opplysningstype.valueOf(pdlOpplysningstype.toString())
            } catch (iae: IllegalArgumentException) {
                log.info("Mottok livshendelse med opplysningstype ({}) fra PDL. Denne ignoreres av løsningen.", pdlOpplysningstype.toString())
                return Livshendelse.Opplysningstype.IKKE_STØTTET
            }
        }
    }

    private fun konvertereEndringstype(pdlEndringstype: Endringstype?): Livshendelse.Endringstype {
        if (pdlEndringstype == null) {
            log.error("Endringstype i mottatt melding var null. Avbryter prosessering")
            throw HendelsemottakException("Endringstype i mottatt melding var null!")
        } else {
            try {
                return Livshendelse.Endringstype.valueOf(pdlEndringstype.name)
            } catch (iae: IllegalArgumentException) {
                log.error("Mottok ukjent endringstype ({}) fra PDL", pdlEndringstype.name)
                iae.printStackTrace()
                throw HendelsemottakException("Ukjent endringstype: ${pdlEndringstype}")
            }
        }
    }

    private fun henteAdressebeskyttelse(adressebeskyttelse: Adressebeskyttelse?): Livshendelse.Gradering {
        return if (adressebeskyttelse == null) {
            Livshendelse.Gradering.UGRADERT
        } else {
            Livshendelse.Gradering.valueOf(adressebeskyttelse.gradering.name)
        }
    }

    private fun henteDødsdato(doedsfall: Doedsfall?): LocalDate? {
        return if (doedsfall == null) {
            null;
        } else {
            doedsfall.doedsdato;
        }
    }

    private fun henteFlyttedato(bostedsadresse: Bostedsadresse?): LocalDate? {
        return if (bostedsadresse == null) {
            null
        } else {
            if (bostedsadresse.angittFlyttedato != null) {
                bostedsadresse.angittFlyttedato
            } else {
                LocalDate.now()
            }
        }
    }

    private fun henteFolkeregisteridentifikator(folkeregisteridentifikator: no.nav.person.pdl.leesah.folkeregisteridentifikator.Folkeregisteridentifikator?): Folkeregisteridentifikator? {
        return if (folkeregisteridentifikator == null) {
            null
        } else {
            Folkeregisteridentifikator(
                folkeregisteridentifikator.identifikasjonsnummer.toString(),
                folkeregisteridentifikator.type.toString(),
                folkeregisteridentifikator.status.toString()
            )
        }
    }

    private fun henteFødsel(foedsel: no.nav.person.pdl.leesah.foedsel.Foedsel?): Fødsel? {
        return if (foedsel == null) {
            null
        } else {
            Fødsel(foedsel.foedeland?.toString(), foedsel.foedselsdato)
        }
    }

    private fun henteInnflytting(innflytting: no.nav.person.pdl.leesah.innflytting.InnflyttingTilNorge?): Innflytting? {
        return if (innflytting == null) {
            null
        } else {
            Innflytting(innflytting.fraflyttingsland?.toString(), innflytting.fraflyttingsstedIUtlandet?.toString())
        }
    }

    private fun henteNavn(navn: no.nav.person.pdl.leesah.navn.Navn?): Navn? {
        return if (navn == null) {
            null
        } else {
            var originaltNavn = OriginaltNavn(
                navn.originaltNavn?.fornavn?.toString(),
                navn.originaltNavn?.mellomnavn?.toString(),
                navn.originaltNavn?.etternavn?.toString(),
            )
            Navn(navn.fornavn?.toString(), navn.mellomnavn?.toString(), navn.etternavn?.toString(), originaltNavn, navn.gyldigFraOgMed)
        }
    }

    private fun henteUtflytting(utflytting: no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge?): Utflytting? {
        return if (utflytting == null) {
            null
        } else {
            Utflytting(utflytting.tilflyttingsland?.toString(), utflytting.tilflyttingsstedIUtlandet?.toString(), utflytting.utflyttingsdato)
        }
    }

    private fun henteSivilstand(sivilstand: no.nav.person.pdl.leesah.sivilstand.Sivilstand?): Sivilstand? {
        return if (sivilstand == null) {
            null
        } else {
            Sivilstand(sivilstand.type?.toString(), sivilstand.bekreftelsesdato, sivilstand.gyldigFraOgMed)
        }
    }

    private fun henteVerge(verge: no.nav.person.pdl.leesah.verge.VergemaalEllerFremtidsfullmakt?): VergeEllerFremtidsfullmakt? {
        return if (verge == null) {
            null
        } else {
            var vergeEllerFullmektig = VergeEllerFullmektig(
                verge.vergeEllerFullmektig.motpartsPersonident.toString(),
                verge.vergeEllerFullmektig.omfang.toString(),
                verge.vergeEllerFullmektig.omfangetErInnenPersonligOmraade
            )
            VergeEllerFremtidsfullmakt(verge.type.toString(), verge.embete.toString(), vergeEllerFullmektig)
        }
    }

    companion object {
        val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")
        val log: Logger = LoggerFactory.getLogger(Livshendelsemottak::class.java)
    }
}


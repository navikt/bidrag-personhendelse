package no.nav.bidrag.person.hendelse.integrasjon.mottak

import no.nav.bidrag.person.hendelse.domene.*
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.bostedsadresse.Bostedsadresse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.LocalDate
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


        val livshendelse = Livshendelse(
            personhendelse?.hendelseId as String,
            cr.offset(),
            personhendelse?.opprettet.toString(),
            personhendelse?.master as String,
            personhendelse?.opplysningstype as String,
            personhendelse.endringstype.name,
            personhendelse?.personidenter?.stream()?.map(CharSequence::toString)?.collect(Collectors.toList()),
            henteDødsdato(personhendelse.doedsfall),
            henteFlyttedato(personhendelse.bostedsadresse),
            henteFolkeregisteridentifikator(personhendelse.folkeregisteridentifikator),
            henteFødsel(personhendelse.foedsel),
            henteInnflytting(personhendelse.innflyttingTilNorge),
            henteNavn(personhendelse.navn),
            henteUtflytting(personhendelse.utflyttingFraNorge),
            personhendelse.tidligereHendelseId as String?,
            henteSivilstand(personhendelse.sivilstand)
        )

        try {
            MDC.put(MdcKonstanter.MDC_KALLID, livshendelse.hendelseid)
            livshendelsebehandler.prosesserNyHendelse(livshendelse)
        } catch (e: RuntimeException) {
            SECURE_LOGGER.error("Feil i prosessering av leesah-hendelser", e)
            throw RuntimeException("Feil i prosessering av leesah-hendelser")
        } finally {
            MDC.clear()
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
            return null
        } else {
            return Folkeregisteridentifikator(
                folkeregisteridentifikator.identifikasjonsnummer as String,
                folkeregisteridentifikator.type as String,
                folkeregisteridentifikator.status as String
            )
        }
    }

    private fun henteFødsel(foedsel: no.nav.person.pdl.leesah.foedsel.Foedsel?): Fødsel? {
        return if (foedsel == null) {
            null
        } else {
            Fødsel(foedsel.foedeland as String, foedsel.foedselsdato)
        }
    }

    private fun henteInnflytting(innflytting: no.nav.person.pdl.leesah.innflytting.InnflyttingTilNorge?): Innflytting? {
        return if (innflytting == null) {
            null
        } else {
            Innflytting(innflytting.fraflyttingsland as String, innflytting.fraflyttingsstedIUtlandet as String)
        }
    }

    private fun henteNavn(navn: no.nav.person.pdl.leesah.navn.Navn?): Navn? {
        return if (navn == null) {
            null
        } else {
            var originaltNavn = OriginaltNavn(
                navn.originaltNavn?.fornavn as String?,
                navn.originaltNavn?.mellomnavn as String?,
                navn.originaltNavn?.etternavn as String?
            )
            Navn(navn.fornavn as String?, navn.mellomnavn as String?, navn.etternavn as String?, originaltNavn, navn.gyldigFraOgMed)
        }
    }

    private fun henteUtflytting(utflytting: no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge?): Utflytting? {
        return if (utflytting == null) {
            null
        } else {
            Utflytting(utflytting.tilflyttingsland as String?, utflytting.tilflyttingsstedIUtlandet as String?, utflytting.utflyttingsdato)
        }
    }

    private fun henteSivilstand(sivilstand: no.nav.person.pdl.leesah.sivilstand.Sivilstand?): Sivilstand? {
        return if (sivilstand == null) {
            null
        } else {
            Sivilstand(sivilstand.type as String, sivilstand.bekreftelsesdato)
        }
    }

    companion object {
        val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")
        val log: Logger = LoggerFactory.getLogger(Livshendelsemottak::class.java)
    }
}

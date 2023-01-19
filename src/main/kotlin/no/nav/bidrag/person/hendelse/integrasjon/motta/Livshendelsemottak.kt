package no.nav.bidrag.person.hendelse.integrasjon.motta

import no.nav.bidrag.person.hendelse.domene.*
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    fun listen(cr: ConsumerRecord<String, Personhendelse>, ack: Acknowledgment) {
        log.info("Livshendelse med hendelseid {} mottatt.", cr.value().henteHendelseId())
        SECURE_LOGGER.info("Har mottatt leesah-hendelse $cr")

        val livshendelse = Livshendelse(
            cr.value().henteHendelseId(),
            cr.offset(),
            cr.value().henteOpprettetTidspunkt(),
            cr.value().henteMaster(),
            cr.value().henteOpplysningstype(),
            cr.value().henteEndringstype(),
            cr.value().hentePersonidenter(),
            cr.value().henteDødsdato(),
            cr.value().henteFlyttedato(),
            cr.value().henteFolkeregisteridentifikator(),
            cr.value().henteFødsel(),
            cr.value().henteInnflytting(),
            cr.value().henteNavn(),
            cr.value().hentUtflytting(),
            cr.value().henteTidligereHendelseId(),
            cr.value().henteSivilstand()
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

        ack.acknowledge()
    }

    private fun GenericRecord.henteOffset() = get("offset").toString().toLong()
    private fun GenericRecord.henteMaster() = get("master").toString()
    private fun GenericRecord.henteOpprettetTidspunkt() = get("opprettet").toString()
        //Instant.ofEpochMilli(Integer.parseInt(get("opprettet").toString()).toLong()).atZone(ZoneId.systemDefault()).toLocalDateTime()

    private fun GenericRecord.henteOpplysningstype() = get("opplysningstype").toString()
    private fun GenericRecord.hentePersonidenter() = (get("personidenter") as GenericData.Array<*>).map { it.toString() }
    private fun GenericRecord.henteEndringstype() = get("endringstype").toString()
    private fun GenericRecord.henteHendelseId() = get("hendelseId").toString()
    private fun GenericRecord.henteDødsdato(): LocalDate? {
        return deserialiserDatofeltFraSubrecord("doedsfall", "doedsdato")
    }

    private fun GenericRecord.henteFlyttedato(): LocalDate? {
        return deserialiserDatofeltFraSubrecord("bostedsadresse", "angittFlyttedato")
    }

    private fun GenericRecord.henteFolkeregisteridentifikator(): Folkeregisteridentifikator? {
        return get("Folkeregisteridentifikator") as Folkeregisteridentifikator
    }

    private fun GenericRecord.henteFødsel(): Fødsel? {
        return get("foedsel") as Fødsel
    }

    private fun GenericRecord.henteInnflytting(): Innflytting? {
        return get("InnflyttingTilNorge") as Innflytting
    }

    private fun GenericRecord.henteTidligereHendelseId(): String? {
        return get("tidligereHendelseId")?.toString()
    }

    private fun GenericRecord.henteNavn(): Navn? {
        return get("navn") as Navn
    }

    private fun GenericRecord.hentUtflytting(): Utflytting? {
        return get("utflyttingFraNorge") as Utflytting
    }

    private fun GenericRecord.henteSivilstand(): Sivilstand? {
        return get("sivilstand") as Sivilstand
    }

    private fun GenericRecord.deserialiserDatofeltFraSubrecord(
        subrecord: String,
        datofelt: String
    ): LocalDate? {
        return try {
            val dato = (get(subrecord) as GenericRecord?)?.get(datofelt)

            // Integrasjonstester bruker EmbeddedKafka, der en datoverdi tolkes direkte som en LocalDate.
            // I prod tolkes datoer som en Integer.
            when (dato) {
                null -> null
                is LocalDate -> dato
                else -> LocalDate.ofEpochDay((dato as Int).toLong())
            }
        } catch (exception: Exception) {
            log.error("Deserialisering av $datofelt feiler")
            throw exception
        }
    }

    companion object {
        val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")
        val log: Logger = LoggerFactory.getLogger(Livshendelsemottak::class.java)
    }
}

package no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic

import com.google.gson.GsonBuilder
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.exception.PubliseringFeiletException
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene.Endringsmelding
import org.apache.kafka.common.KafkaException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BidragKafkaMeldingsprodusent(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val databasetjeneste: Databasetjeneste,

) {
    @Transactional
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    fun publisereEndringsmelding(aktørid: String, personidenter: Set<String>) {
        publisereMelding(BIDRAG_PERSONHENDELSE_TOPIC, aktørid, personidenter)
    }

    private fun publisereMelding(emne: String, aktørid: String, personidenter: Set<String>) {
        slog.info("Publiserer endringsmelding for aktørid $aktørid")
        val melding = tilJson(Endringsmelding(aktørid, personidenter))
        try {
            val future = kafkaTemplate.send(emne, aktørid, melding)

            future.whenComplete { result, ex ->
                if (ex != null) {
                    log.warn("Publisering av melding til topic $BIDRAG_PERSONHENDELSE_TOPIC feilet.")
                    slog.warn("Publisering av melding for aktørid ${result.producerRecord.key()} til topic $BIDRAG_PERSONHENDELSE_TOPIC feilet.")
                    throw ex
                }
            }

            databasetjeneste.oppdaterePubliseringstidspunkt(aktørid)
            databasetjeneste.oppdatereStatusPåHendelserEtterPublisering(aktørid)
        } catch (e: KafkaException) {
            // Fanger exception for å unngå at meldingsinnhold logges i åpen logg.
            slog.error("Publisering av melding for aktørid $aktørid feilet med feilmedlding: ${e.message}")
            throw PubliseringFeiletException("Publisering av melding med nøkkel $aktørid til topic $BIDRAG_PERSONHENDELSE_TOPIC feilet.")
        }
    }

    companion object {
        val BIDRAG_PERSONHENDELSE_TOPIC = "bidrag.personhendelse.v1"
        private val log = LoggerFactory.getLogger(this::class.java)
        private val slog: Logger = LoggerFactory.getLogger("secureLogger")

        fun tilJson(endringsmelding: Endringsmelding): String {
            val gsonbuilder = GsonBuilder()
            val gson = gsonbuilder.create()
            return gson.toJson(endringsmelding)
        }
    }
}

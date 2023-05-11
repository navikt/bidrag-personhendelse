package no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.person.hendelse.database.Aktor
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene.Endringsmelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class BidragKafkaMeldingsprodusent(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val databasetjeneste: Databasetjeneste

) {
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    fun publisereEndringsmelding(endringsmelding: Endringsmelding) {
        var melding = objectMapper.writeValueAsString(endringsmelding)
        publisereMelding(BIDRAG_PERSONHENDELSE_TOPIC, endringsmelding.aktør, melding)
    }

    private fun publisereMelding(emne: String, aktør: Aktor, data: String) {
        slog.info("Publiserer endringsmelding for aktørid ${aktør.aktorid}")
        val melding = objectMapper.writeValueAsString(data)
        var future = kafkaTemplate.send(emne, aktør.aktorid, melding)

        future.whenComplete { result, ex ->
            if (ex != null) {
                log.warn("Publisering av melding til topic $BIDRAG_PERSONHENDELSE_TOPIC feilet.")
                slog.warn("Publisering av melding for aktørid ${result.producerRecord.key()} til topic $BIDRAG_PERSONHENDELSE_TOPIC feilet.")
            } else {
                databasetjeneste.oppdaterePubliseringstidspunkt(aktør.aktorid)
                databasetjeneste.oppdatereStatusPåHendelserEtterPublisering(aktør.aktorid)
                databasetjeneste.oppdatereStatusPåKontoendringerEtterPublisering(aktør.aktorid)
            }
        }
    }

    companion object {
        val BIDRAG_PERSONHENDELSE_TOPIC = "bidrag.personhendelse.v1"
        private val log = LoggerFactory.getLogger(this::class.java)
        private val slog: Logger = LoggerFactory.getLogger("secureLogger")
    }
}

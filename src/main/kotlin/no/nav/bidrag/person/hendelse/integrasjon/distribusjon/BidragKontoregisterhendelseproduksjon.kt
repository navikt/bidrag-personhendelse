package no.nav.bidrag.person.hendelse.integrasjon.distribusjon

import no.nav.person.endringsmelding.v1.Endringsmelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class BidragKontoregisterhendelseproduksjon(val kafkaTemplate: KafkaTemplate<String, String>) {

    fun publisereEndringsmeldingTilBidragTopic(endringsmelding: Endringsmelding) {
        kafkaTemplate.send(BIDRAG_PERSONHENDELSE_TOPIC, endringsmelding.kontohaver.toString()).get(30, TimeUnit.SECONDS)
        LOG.info("Endringsmelding sendt til ${BIDRAG_PERSONHENDELSE_TOPIC}")
    }

    companion object {
        val BIDRAG_PERSONHENDELSE_TOPIC = "bidrag.personhendelse.v1"
        val LOG: Logger = LoggerFactory.getLogger(BidragKontoregisterhendelseproduksjon::class.java)
        val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
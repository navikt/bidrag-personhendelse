package no.nav.bidrag.person.hendelse.integrasjon.motta

import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class Identhendelsemottak {

    @KafkaListener(
        groupId = "aktor-v1.bidrag",
        topics = ["pdl.aktor-v1"],
        id = "bidrag-person-hendelse,aktor-v1",
        idIsGroup = false,
        containerFactory = "kafkaIdenthendelseListenerContainerFactory"
    )
    fun listen(consumerRecord: ConsumerRecord<String, Aktor?>, ack: Acknowledgment) {
        try {
            log.info("Aktørhendelse mottatt")
            SECURE_LOGGER.info("Har mottatt ident-hendelse $consumerRecord")

            val aktoer = consumerRecord.value()

            if (aktoer == null) {
                log.warn("Tom aktør fra identhendelse")
                SECURE_LOGGER.warn("Tom aktør fra identhendelse med noekkel ${consumerRecord.key()}")
            }

            aktoer?.identifikatorer?.singleOrNull { ident ->
                ident.type == Type.FOLKEREGISTERIDENT && ident.gjeldende
            }?.also { folkeregisterident ->
                SECURE_LOGGER.info("Sender ident-hendelse til ba-sak for ident $folkeregisterident")
            }
        } catch (e: RuntimeException) {
            log.warn("Feil i prosessering av ident-hendelser", e)
            SECURE_LOGGER.warn("Feil i prosessering av ident-hendelser $consumerRecord", e)
            throw RuntimeException("Feil i prosessering av ident-hendelser")
        }
        ack.acknowledge()
    }

    companion object {

        val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLogger")
        val log: Logger = LoggerFactory.getLogger(Identhendelsemottak::class.java)
    }
}

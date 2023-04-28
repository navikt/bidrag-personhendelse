package no.nav.bidrag.person.hendelse.integrasjon.kontoregister

import no.nav.bidrag.person.hendelse.prosess.Kontoendringsbehandler
import no.nav.person.endringsmelding.v1.Endringsmelding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(
    value = ["funksjonsbrytere.kafka.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class Kontoendringsmottak(val kontoendringsbehandler: Kontoendringsbehandler) {
    @KafkaListener(
        groupId = "kontoregister-person-endringsmelding-v2.bidrag",
        topics = ["okonomi.kontoregister-person-endringsmelding.v2"],
        id = "bidrag-person-hendelse-kontoregister-person-endringsmelding-v2",
        idIsGroup = false
    )
    fun listen(@Payload endringsmelding: Endringsmelding, cr: ConsumerRecord<String, Endringsmelding>) {
        slog.info(
            "Kontoregisterendringsmelding mottatt: Record key={}, value={}, value={}",
            cr.key(),
            cr.value(),
            cr.offset()
        )
        kontoendringsbehandler.lagreKontoendring(endringsmelding.kontohaver.toString())
        slog.info("Kontoendring lagret for kontoeier {}", endringsmelding.kontohaver)
    }

    companion object {
        val slog: Logger = LoggerFactory.getLogger("secureLogger")
    }
}

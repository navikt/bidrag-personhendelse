package no.nav.bidrag.person.hendelse.integrasjon.distribusjon

import no.nav.bidrag.person.hendelse.exception.OverføringFeiletException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.core.JmsTemplate
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Component
open class Meldingsprodusent(private val jmsTemplate: JmsTemplate) {

    @Retryable(value = [java.lang.Exception::class], backoff = Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000))
    fun sendeMelding(mottakerkoe: String, melding: String) {
        secureLogger.info("Sender melding til {} med innhold: {}", mottakerkoe, melding)
        try {
            jmsTemplate.send(mottakerkoe) { s -> s.createTextMessage(melding) }
        } catch(e:Exception) {
            logger.error("Sending av melding til WMQ feilet med feilmelding '{}'", e.message)
            throw e.message?.let { OverføringFeiletException(it) }!!
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Meldingsprodusent::class.java)
        private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}

package no.nav.bidrag.person.hendelse.integrasjon.bidrag.person

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.PersonidentDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragPersonklient(
    @Value("\${egenskaper.integrasjon.bidrag-person.url}") val bidragPersonUrl: URI,
    @Qualifier("azure") val restTemplate: RestTemplate
) : AbstractRestClient(restTemplate, "bidrag-person") {

    private fun createUri(path: String?) = UriComponentsBuilder.fromUri(bidragPersonUrl)
        .path(path ?: "").build().toUri()

    @Retryable(value = [Exception::class], maxAttempts = 10, backoff = Backoff(delay = 1000, multiplier = 2.0))
    fun henteAlleIdenterForPerson(personIdent: String): List<PersonidentDto>? {
        return try {
            postForEntity(createUri(ENDEPUNKT_PERSONIDENTER), HentePersonidenterRequest(personIdent))
        } catch (e: HttpStatusCodeException) {
            log.warn("Kall mot bidrag-person for å hente alle registrerte personidenter for personident feilet med statuskode ${e.statusCode} og melding ${e.message}")
            slog.warn("Kall mot bidrag-person for å hente alle registrerte personidenter for personident $personIdent feilet med statuskode ${e.statusCode} og melding ${e.message}")
            throw e
        }
    }

    companion object {
        const val ENDEPUNKT_PERSONIDENTER = "/personidenter"
        val log: Logger = LoggerFactory.getLogger(this::class.java)
        val slog: Logger = LoggerFactory.getLogger("secureLogger")
    }
}

package no.nav.bidrag.person.hendelse.integrasjon.bidrag.person

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.PersonidentDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
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
        return postForEntity(createUri(ENDEPUNKT_PERSONIDENTER), HentePersonidenterRequest(personIdent))
    }

    companion object {
        const val ENDEPUNKT_PERSONIDENTER = "/personidenter"
    }
}

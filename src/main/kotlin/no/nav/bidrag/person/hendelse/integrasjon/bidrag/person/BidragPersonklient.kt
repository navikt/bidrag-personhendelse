package no.nav.bidrag.person.hendelse.integrasjon.bidrag.person

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.domene.PersonidentDto
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.domene.PersonidenterDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragPersonklient(
    @Value("\${BIDRAG_PERSON_URL}") bidragPersonBaseUrl: URI,
    @Qualifier("azure") private val restTemplate: RestOperations
) : AbstractRestClient(restTemplate, "bidragPerson") {

    private val bidragPersonUri =
        UriComponentsBuilder.fromUri(bidragPersonBaseUrl).pathSegment("fodselsdatoer").build().toUri()

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    fun henteAlleIdenterForPerson(personIdent: String): Set<PersonidentDto> {
        val personidenter: PersonidenterDto? = postForEntity(bidragPersonUri, personIdent)
        if (personidenter != null) {
            return personidenter.identer
        } else {
            return emptySet()
        }
    }
}

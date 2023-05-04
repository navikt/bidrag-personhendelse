package no.nav.bidrag.person.hendelse.integrasjon.bidrag.person

import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.domene.PersonidentDto
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.domene.PersonidenterDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragPersonklient(
    @Qualifier("bidrag-person-azure-client-credentials") clientCredentialsRestTemplate: RestTemplate
) {

    var clientCredentialsRestTemplate: RestTemplate = clientCredentialsRestTemplate

    @Retryable(value = [Exception::class], maxAttempts = 10, backoff = Backoff(delay = 1000, multiplier = 2.0))
    fun henteAlleIdenterForPerson(personIdent: String): Set<PersonidentDto> {
        var respons: ResponseEntity<PersonidenterDto> =
            clientCredentialsRestTemplate.exchange("/fo", HttpMethod.POST, HttpEntity(personIdent))

        if (respons.body != null) {
            return respons.body.identer
        } else {
            return emptySet()
        }
    }

    @Retryable(value = [Exception::class], maxAttempts = 10, backoff = Backoff(delay = 1000, multiplier = 2.0))
    fun henteAlleIdenterForPersoner(personidenter: Set<String>) : Set<PersonidenterDto> {
        var respons: ResponseEntity<Set<PersonidenterDto>> =
            clientCredentialsRestTemplate.exchange("/fo", HttpMethod.POST, HttpEntity(personidenter))

        if (respons.body != null) {
            return respons.body.toSet()
        } else {
            return emptySet()
        }
    }
}

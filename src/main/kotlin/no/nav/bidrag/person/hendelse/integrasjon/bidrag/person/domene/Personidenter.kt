package no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.domene

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.person.hendelse.integrasjon.pdl.domene.Identgruppe

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonidenterDto(
    val identer: Set<PersonidentDto>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonidentDto(
    val ident: String,
    val gruppe: Identgruppe,
    val historisk: Boolean
)
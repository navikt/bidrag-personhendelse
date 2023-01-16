package no.nav.bidrag.person.hendelse.domene

data class Folkeregisteridentifikator (
    val identifikasjonsnummer: String,
    val type: String,
    val status: String
)
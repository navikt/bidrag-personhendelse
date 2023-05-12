package no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene

data class Endringsmelding(
    val aktørid: String,
    val personidenter: Set<String>
)

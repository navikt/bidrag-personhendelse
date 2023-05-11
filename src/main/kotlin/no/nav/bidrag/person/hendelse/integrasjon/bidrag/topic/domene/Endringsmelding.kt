package no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene

import no.nav.bidrag.person.hendelse.database.Aktor

data class Endringsmelding(
    val aktør: Aktor,
    val personidenter: String
)

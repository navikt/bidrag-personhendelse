package no.nav.bidrag.person.hendelse.domene

import java.time.LocalDate

data class Fødsel (
    val fødeland: String?  = null,
    val fødselsdato: LocalDate = LocalDate.now()
)
package no.nav.bidrag.person.hendelse.domene

import java.time.LocalDate

data class Sivilstand (
    val sivilstand: String? = null,
    val sivilstandDato: LocalDate = LocalDate.now()
)
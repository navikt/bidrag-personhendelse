package no.nav.bidrag.person.hendelse.domene

import java.time.LocalDate

data class Utflytting(
    val tilflyttingsland: String? = null,
    val tilflyttingsstedIUtlandet: String? = null,
    val utflyttingsdato: LocalDate? = null
)

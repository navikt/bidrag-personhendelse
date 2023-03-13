package no.nav.bidrag.person.hendelse.domene
import java.time.LocalDate

data class Navn (
    val fornavn: String? = null,
    val mellomnavn: String? = null,
    val etternavn: String? = null,
    val originaltNavn: OriginaltNavn? = null,
    val gyldigFraOgMed: LocalDate? = null
)

data class OriginaltNavn(
    val fornavn: String? = null,
    val mellomnavn: String? = null,
    val etternavn: String? = null
)



package no.nav.bidrag.person.hendelse.domene

data class VergeEllerFremtidsfullmakt(
    val type: String? = null,
    val embete: String? = null,
    val vergeEllerFullmektig: VergeEllerFullmektig? = null
)

data class VergeEllerFullmektig(
    val motpartsPersonident: String? = null,
    val omfang: String? = null,
    val omfangetErInnenPersonligOmraade: Boolean? = false
)

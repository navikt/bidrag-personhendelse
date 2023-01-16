package no.nav.bidrag.person.hendelse.domene

import java.time.LocalDate

data class Livshendelse(

    val hendelseid: String,
    val offset: Long,
    val opprettet: String,
    val master: String,
    val opplysningstype: String,
    val endringstype: String,
    val personidenter: List<String>,
    val doedsdato: LocalDate? = null,
    val flyttedato: LocalDate? = LocalDate.now(),
    val folkeregisteridentifikator: Folkeregisteridentifikator? = null,
    val fødsel: Fødsel? = null,
    val innflytting: Innflytting? = null,
    val navn: Navn? = null,
    val utflytting: Utflytting? = null,
    val tidligereHendelseid: String? = null,
    val sivilstand: Sivilstand? = null,
    val verge: VergeEllerFremtidsfullmakt? = null,
    val addressebeskyttelse: Gradering = Gradering.UGRADERT

) {

    fun hentPersonident() = personidenter.first { it.length == 11 }
    fun hentGjeldendeAktørid() = personidenter.first { it.length == 13 }
    fun hentPersonidenter() = personidenter?.filter { it.length == 11 }

    enum class Gradering {
        STRENGT_FORTROLIG_UTLAND,
        STRENGT_FORTROLIG,
        FORTROLIG,
        UGRADERT
    }

}

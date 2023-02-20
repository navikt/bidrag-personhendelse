package no.nav.bidrag.person.hendelse.domene

import com.google.gson.GsonBuilder
import no.nav.bidrag.person.hendelse.prosess.LocalDateTimeTypeAdapter
import no.nav.bidrag.person.hendelse.prosess.LocalDateTypeAdapter
import java.time.LocalDate
import java.time.LocalDateTime

data class Livshendelse(

    val hendelseid: String,
    val opplysningstype: Opplysningstype,
    val endringstype: Endringstype,
    val personidenter: List<String>?,
    val tidligereHendelseid: String? = null,
    val doedsdato: LocalDate? = null,
    val flyttedato: LocalDate? = LocalDate.now(),
    val folkeregisteridentifikator: Folkeregisteridentifikator? = null,
    val foedsel: Foedsel? = null,
    val innflytting: Innflytting? = null,
    val navn: Navn? = null,
    val utflytting: Utflytting? = null,
    val sivilstand: Sivilstand? = null,
    val verge: VergeEllerFremtidsfullmakt? = null,
    val adressebeskyttelse: Gradering = Gradering.UGRADERT,
    val offset: Long = 0L,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val master: String = "PDL"

) {

    fun hentPersonident() = personidenter?.first { it.length == 11 }
    fun hentGjeldendeAktørid() = personidenter?.first { it.length == 13 }
    fun hentPersonidenter() = personidenter?.filter { it.length == 11 }

    enum class Gradering {
        STRENGT_FORTROLIG_UTLAND,
        STRENGT_FORTROLIG,
        FORTROLIG,
        UGRADERT
    }

    enum class Endringstype {
        OPPRETTET, KORRIGERT, ANNULLERT, OPPHOERT
    }

    enum class Opplysningstype {
        ADRESSEBESKYTTELSE_V1, BOSTEDSADRESSE_V1, DOEDSFALL_V1, FOEDSEL_V1, FOLKEREGISTERIDENTIFIKATOR_V1, INNFLYTTING_TIL_NORGE, NAVN_V1, UTFLYTTING_FRA_NORGE, SIVILSTAND_V1, VERGE_V1, IKKE_STØTTET
    }

    companion object {
        fun tilJson(livshendelse: Livshendelse): String {
            var gsonbuilder = GsonBuilder()
            gsonbuilder.registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter().nullSafe())
            gsonbuilder.registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter().nullSafe())
            var gson = gsonbuilder.create()
            return gson.toJson(livshendelse)
        }
    }
}

package no.nav.bidrag.person.hendelse.domene

import java.time.LocalDate

data class Livshendelse(
    val hendelseId: String,
    val gjeldendeAktørId: String,
    val offset: Long,
    val opplysningstype: String,
    val endringstype: String,
    val personIdenter: List<String>,
    val dødsdato: LocalDate? = null,
    val fødselsdato: LocalDate? = null,
    val fødeland: String? = null,
    val utflyttingsdato: LocalDate? = null,
    val tidligereHendelseId: String? = null,
    val sivilstand: String? = null,
    val sivilstandDato: LocalDate? = null,
) {

    fun hentPersonidenter() = personIdenter.filter { it.length == 11 }
}

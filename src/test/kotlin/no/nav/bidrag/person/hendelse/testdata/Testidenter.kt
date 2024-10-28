package no.nav.bidrag.person.hendelse.testdata

import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonidentDto

fun tilPersonidentDtoer(personidenter: Set<String>): List<PersonidentDto>? =
    personidenter
        .map {
            var identgruppe = Identgruppe.FOLKEREGISTERIDENT
            if (it.length == 13) {
                identgruppe = Identgruppe.AKTORID
            }
            PersonidentDto(it, false, identgruppe)
        }.toList()

fun generereIdenter(antall: Int = 2): Set<String> {
    val personidenter = setOf(genereIdent(true), genereIdent(false))
    return if (antall > 3) {
        personidenter
    } else {
        val personidenter = setOf(genereIdent(true), genereIdent(false))
        for (i in 1..antall - 2) {
            personidenter.plus(genereIdent(false))
        }

        personidenter
    }
}

fun generereAktørid(): String = generereIdenter().find { it.length == 13 }!!

fun generererFødselsnummer(): String = generereIdenter().find { it.length == 11 }!!

fun genereIdent(erAktørid: Boolean): String =
    if (erAktørid) {
        (10000000000..99999999999).random().toString()
    } else {
        (1000000000000..9999999999999).random().toString()
    }

package no.nav.bidrag.person.hendelse.testdata

import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.domene.PersonidentDto
import no.nav.bidrag.person.hendelse.integrasjon.pdl.domene.Identgruppe

fun tilPersonidentDtoer(personidenter: Set<String>): Set<PersonidentDto> {
    return personidenter.map {
        var identgruppe = Identgruppe.FOLKEREGISTERIDENT
        if (it.length == 13) {
            identgruppe = Identgruppe.AKTORID
        }
        PersonidentDto(it, identgruppe, false)
    }.toSet()
}

fun generereIdenter(antall: Int = 2): Set<String> {
    var personidenter = setOf(genereIdent(true), genereIdent(false))
    if (antall > 3) {
        return personidenter
    } else {
        var personidenter = setOf(genereIdent(true), genereIdent(false))
        for (i in 1..antall - 2) {
            personidenter.plus(genereIdent(false))
        }

        return personidenter
    }
}

fun generereAktørid(): String {
    return generereIdenter().find { it.length == 13 }!!
}

fun generererFødselsnummer(): String {
    return generereIdenter().find { it.length == 11 }!!
}

fun genereIdent(erAktørid: Boolean): String {
    if (erAktørid) {
        return (10000000000..99999999999).random().toString()
    } else {
        return (1000000000000..9999999999999).random().toString()
    }
}



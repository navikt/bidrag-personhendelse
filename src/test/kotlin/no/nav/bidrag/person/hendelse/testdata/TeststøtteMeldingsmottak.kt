package no.nav.bidrag.person.hendelse.testdata

import no.nav.bidrag.person.hendelse.database.Aktor
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Hendelsemottak
import no.nav.bidrag.person.hendelse.database.Kontoendring
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.zip.CRC32

@Component
class TeststøtteMeldingsmottak(val databasetjeneste: Databasetjeneste) {

    fun henteAktør(aktørid: String): Aktor {
        val eksisteredeAktør = databasetjeneste.aktorDao.findByAktorid(aktørid)

        if (eksisteredeAktør.isPresent) {
            return eksisteredeAktør.get()
        } else {
            return databasetjeneste.aktorDao.save(Aktor(aktørid))
        }
    }

    fun oppretteOgLagreKontoendring(
        personidenter: List<String>,
        mottatt: LocalDateTime = LocalDateTime.now()
            .minusHours(databasetjeneste.egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 1),
        publisert: LocalDateTime? = null
    ): Kontoendring {
        val aktør = henteAktør(personidenter.first { it.length == 13 })
        return databasetjeneste.kontoendringDao.save(Kontoendring(aktør, personidenter.joinToString { it }, mottatt))
    }

    fun oppretteOgLagreHendelsemottak(personidenter: List<String>, status: Status = Status.OVERFØRT): Hendelsemottak {
        val aktør = henteAktør(personidenter.first { it.length == 13 })

        var mottattHendelse = Hendelsemottak(
            CRC32().value.toString(),
            Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
            Livshendelse.Endringstype.OPPRETTET,
            personidenter.joinToString { it },
            aktør
        )

        mottattHendelse.status = status

        return databasetjeneste.hendelsemottakDao.save(mottattHendelse)
    }
}

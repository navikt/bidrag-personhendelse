package no.nav.bidrag.person.hendelse.testdata

import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Hendelsemottak
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import org.springframework.stereotype.Component
import java.util.zip.CRC32

@Component
class TeststøtteMeldingsmottak(val databasetjeneste: Databasetjeneste) {

    fun oppretteOgLagreHendelsemottak(personidenter: List<String>, status: Status = Status.OVERFØRT): Hendelsemottak {
        val aktørid = personidenter.first { it.length == 13 }

        var mottattHendelse = Hendelsemottak(
            CRC32().value.toString(),
            Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
            Livshendelse.Endringstype.OPPRETTET,
            personidenter.joinToString { it },
            aktørid
        )

        mottattHendelse.status = status

        return databasetjeneste.hendelsemottakDao.save(mottattHendelse)
    }
}

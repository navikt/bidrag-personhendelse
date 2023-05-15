package no.nav.bidrag.person.hendelse.database

import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Egenskaper
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class Databasetjeneste(
    open val hendelsemottakDao: HendelsemottakDao,
    val egenskaper: Egenskaper
) {

    fun oppdatereStatusPåHendelse(id: Long, nyStatus: Status) {
        val hendelse = hendelsemottakDao.findById(id)
        if (hendelse.isPresent) {
            val hendelsemottak = hendelse.get()
            hendelsemottak.status = nyStatus
            hendelsemottak.statustidspunkt = LocalDateTime.now()
            this.hendelsemottakDao.save(hendelsemottak)
        }
    }

    @Transactional
    fun oppdatereStatusPåHendelser(ider: List<Long>, nyStatus: Status) {
        for (id in ider) {
            oppdatereStatusPåHendelse(id, nyStatus)
        }
    }

    @Transactional
    fun oppdatereStatusPåHendelserEtterPublisering(aktørid: String) {
        var ider = hendelsemottakDao.finnHendelsemottakIderMedStatusOverført(aktørid)

        for (id in ider) {
            oppdatereStatusPåHendelse(id, Status.PUBLISERT)
        }
    }

    @Transactional(readOnly = false)
    fun lagreHendelse(livshendelse: Livshendelse): Hendelsemottak {
        var begrensetSettMedPersonidenter = begrenseAntallPersonidenter(livshendelse.personidenter.toSet())

        if (livshendelse.personidenter.size > MAKS_ANTALL_PERSONIDENTER) {
            log.warn(
                "Mottatt livshendelse med hendelseid ${livshendelse.hendelseid} inneholdt over $MAKS_ANTALL_PERSONIDENTER personidenter. " +
                    "Kun de $MAKS_ANTALL_PERSONIDENTER første arkiveres."
            )
        }

        // Kansellere eventuell tidligere hendelse som er lagret i databasen med status mottatt
        var status = kansellereTidligereHendelse(livshendelse)

        // Sørge for at meldinger med endringstype KORRIGERT sendes videre
        if (Status.KANSELLERT == status && Livshendelse.Endringstype.KORRIGERT == livshendelse.endringstype) {
            status = Status.MOTTATT
        }

        // Kansellerer hendelser om opphør av bostedsadresse. Endring av eksisterende bostedsadresse fører til utsending av to hendelser. Opprett for ny adresse og opphør for gammel.
        if (Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1 == livshendelse.opplysningstype && Livshendelse.Endringstype.OPPHOERT == livshendelse.endringstype) {
            status = Status.KANSELLERT
        }

        return hendelsemottakDao.save(
            Hendelsemottak(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
                livshendelse.endringstype,
                begrensetSettMedPersonidenter.joinToString { it },
                livshendelse.aktorid,
                livshendelse.opprettet,
                livshendelse.tidligereHendelseid,
                Livshendelse.tilJson(livshendelse),
                livshendelse.master,
                livshendelse.offset,
                status
            )
        )
    }

    fun hentePubliseringsklareHendelser(): HashMap<String, Set<String>> {
        return tilHashMap(hendelsemottakDao.hentePubliseringsklareOverførteHendelser())
    }

    private fun tilHashMap(liste: Set<Hendelsemottak>): HashMap<String, Set<String>> {
        var map = HashMap<String, Set<String>>()
        liste.forEach {
            map.put(it.aktorid, it.personidenter.split(',').map { ident -> ident.trim() }.toSet())
        }
        return map
    }

    private fun kansellereTidligereHendelse(livshendelse: Livshendelse): Status {
        val tidligereHendelseMedStatusMottatt =
            livshendelse.tidligereHendelseid?.let { hendelsemottakDao.findByHendelseidAndStatus(it, Status.MOTTATT) }
        tidligereHendelseMedStatusMottatt?.status = Status.KANSELLERT
        tidligereHendelseMedStatusMottatt?.statustidspunkt = LocalDateTime.now()

        return if (Status.KANSELLERT == tidligereHendelseMedStatusMottatt?.status) {
            log.info(
                "Livshendelse med hendelseid ${tidligereHendelseMedStatusMottatt.hendelseid} ble erstattet av livshendelse med hendelseid ${livshendelse.hendelseid} og endringstype ${livshendelse.endringstype}."
            )

            if (Livshendelse.Endringstype.KORRIGERT != livshendelse.endringstype) {
                Status.KANSELLERT
            } else {
                Status.MOTTATT
            }
        } else {
            Status.MOTTATT
        }
    }

    companion object {
        const val MAKS_ANTALL_PERSONIDENTER = 20

        val log: Logger = LoggerFactory.getLogger(Livshendelsebehandler::class.java)

        fun begrenseAntallPersonidenter(personidenter: Set<String>): Set<String> {
            if (personidenter.size > MAKS_ANTALL_PERSONIDENTER) return personidenter.take(MAKS_ANTALL_PERSONIDENTER).toSet()
            return personidenter
        }
    }
}

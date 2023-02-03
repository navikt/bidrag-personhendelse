package no.nav.bidrag.person.hendelse.database

import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
open class Databasetjeneste(open val hendelsemottakDao: HendelsemottakDao) {

    open fun henteIdTilHendelserSomSkalOverføresTilBisys(statustidspunktFør: LocalDateTime): Set<Long> {
        hendelsemottakDao.oppdatereStatusPåHendelserSomSkalOverføres(statustidspunktFør)
        return hendelsemottakDao.henteIdTilHendelserSomSkalSendesVidere()
    }

    fun henteHendelse(id: Long): Optional<Hendelsemottak> {
        return hendelsemottakDao.findById(id)
    }

    fun hendelseFinnesIDatabasen(hendelseid: String, opplysningstype: Livshendelse.Opplysningstype): Boolean {
        return hendelsemottakDao.existsByHendelseidAndOpplysningstype(hendelseid, opplysningstype)
    }

    @Transactional
    open fun oppdatereStatus(id: Long, nyStatus: Status) {
        var hendelse = hendelsemottakDao.findById(id)
        if (hendelse.isPresent) {
            var hendelsemottak = hendelse.get()
            hendelsemottak.status = nyStatus
            hendelsemottak.statustidspunkt = LocalDateTime.now()
            hendelsemottakDao.save(hendelsemottak)
        }
    }

    @Transactional(readOnly = false)
    open fun lagreHendelse(livshendelse: Livshendelse): Hendelsemottak {

        var listeMedPersonidenter = livshendelse.personidenter

        if (livshendelse.personidenter?.size!! > Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER) {
            listeMedPersonidenter = listeMedPersonidenter?.subList(0, Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER)
            Livshendelsebehandler.log.warn(
                "Mottatt livshendelse med hendelseid ${livshendelse.hendelseid} inneholdt over ${Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER} personidenter. " +
                        "Kun de ${Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER} første arkiveres."
            )
        }

        // Kansellere eventuell tidligere hendelse som er lagret i databasen med status mottatt
        var status = kansellereTidligereHendelse(livshendelse)

        // Sørge for at meldinger med endringstype KORRIGERT sendes videre
        if (Status.KANSELLERT == status && Livshendelse.Endringstype.KORRIGERT == livshendelse.endringstype) {
            status = Status.MOTTATT
        }

        return hendelsemottakDao.save(
            Hendelsemottak(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
                livshendelse.endringstype,
                listeMedPersonidenter?.joinToString { it },
                livshendelse.tidligereHendelseid,
                Livshendelse.tilJson(livshendelse),
                livshendelse.master,
                livshendelse.offset,
                status
            )
        )
    }

    private fun kansellereTidligereHendelse(livshendelse: Livshendelse): Status {
        var tidligereHendelseMedStatusMottatt = livshendelse.tidligereHendelseid?.let { hendelsemottakDao.findByHendelseidAndStatus(it, Status.MOTTATT) }
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
        val log: Logger = LoggerFactory.getLogger(Livshendelsebehandler::class.java)
    }
}
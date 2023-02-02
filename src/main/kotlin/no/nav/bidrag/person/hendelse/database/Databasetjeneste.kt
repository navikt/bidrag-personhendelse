package no.nav.bidrag.person.hendelse.database

import jakarta.transaction.Transactional
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.prosess.Livshendelsebehandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
open class Databasetjeneste(open val hendelsemottakDao: HendelsemottakDao) {

    @Transactional
    fun kansellereIkkeOverførteAnnullerteHendelser() {

        var ikkeOverførteHendelserSomViserTilTidligereHendelser = hendelsemottakDao.henteIdTilHendelserSomViserTilTidligereHendelser()
        for (id in ikkeOverførteHendelserSomViserTilTidligereHendelser) {
            var nyHendelse = hendelsemottakDao.findById(id)
            var tidligereHendelse = nyHendelse.tidligereHendelseid?.let { hendelsemottakDao.findByHendelseid(it) }
            if (tidligereHendelse != null && Status.MOTTATT == tidligereHendelse.status) {
                when (nyHendelse.endringstype) {
                    Livshendelse.Endringstype.ANNULLERT, Livshendelse.Endringstype.OPPHOERT -> {
                        nyHendelse.status = Status.KANSELLERT
                        nyHendelse.statustidspunkt = LocalDateTime.now()
                        tidligereHendelse.status = Status.KANSELLERT
                        tidligereHendelse.statustidspunkt = LocalDateTime.now()
                        log.info(
                            "Livshendelse med hendelseid ${tidligereHendelse.hendelseid} ble annullert av livshendelse med hendelseid ${nyHendelse.hendelseid} og endringstype ${nyHendelse.endringstype}. Begge livshendelsene får status KANSELLERT, og overføres derfor ikke til Bisys."
                        )
                    }

                    Livshendelse.Endringstype.KORRIGERT -> {
                        tidligereHendelse.status = Status.KANSELLERT
                        tidligereHendelse.statustidspunkt = LocalDateTime.now()
                        log.info(
                            "Livshendelse med hendelseid ${tidligereHendelse.hendelseid} ble erstattet av livshendelse med hendelseid ${nyHendelse.hendelseid} og endringstype ${nyHendelse.endringstype}."
                        )
                    }

                    else -> {
                        log.warn("Endringstype ${nyHendelse.endringstype} skal normalt ikke referere til tidligere hendelser. Ignorerer denne.")
                    }
                }
            }
        }
    }

    fun henteIdTilHendelserSomSkalOverføresTilBisys(statustidspunktFør: LocalDateTime): Set<Int> {
        return hendelsemottakDao.henteIdTilHendelserSomSkalSendesVidere(statustidspunktFør)
    }

    @Transactional
    fun henteHendelse(id: Int): Hendelsemottak {
        var hendelsemottak = hendelsemottakDao.findById(id)
        return hendelsemottak
    }

    fun hendelseFinnesIDatabasen(hendelseid: String, opplysningstype: Livshendelse.Opplysningstype): Boolean {
        return hendelsemottakDao.existsByHendelseidAndOpplysningstype(hendelseid, opplysningstype)
    }

    fun lagreHendelse(livshendelse: Livshendelse): Hendelsemottak {

        var listeMedPersonidenter = livshendelse.personidenter

        if (livshendelse.personidenter?.size!! > Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER) {
            listeMedPersonidenter = listeMedPersonidenter?.subList(0, Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER)
            Livshendelsebehandler.log.warn(
                "Mottatt livshendelse med hendelseid ${livshendelse.hendelseid} inneholdt over ${Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER} personidenter. " +
                        "Kun de ${Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER} første arkiveres."
            )
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
                livshendelse.offset
            )
        )
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Livshendelsebehandler::class.java)
    }
}
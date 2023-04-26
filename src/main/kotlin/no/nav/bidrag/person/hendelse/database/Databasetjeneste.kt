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
    open val aktorDao: AktorDao,
    open val hendelsemottakDao: HendelsemottakDao,
    open val kontoendringDao: KontoendringDao,
    val egenskaper: Egenskaper
) {
    fun oppdatereStatusPåHendelse(id: Long, nyStatus: Status) {
        var hendelse = hendelsemottakDao.findById(id)
        if (hendelse.isPresent) {
            var hendelsemottak = hendelse.get()
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

    @Transactional(readOnly = false)
    fun lagreHendelse(livshendelse: Livshendelse): Hendelsemottak {
        var listeMedPersonidenter = livshendelse.personidenter

        if (livshendelse.personidenter.size > Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER) {
            listeMedPersonidenter = listeMedPersonidenter.subList(0, Livshendelsebehandler.MAKS_ANTALL_PERSONIDENTER)
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

        // Kansellerer hendelser om opphør av bostedsadresse. Endring av eksisterende bostedsadresse fører til utsending av to hendelser. Opprett for ny adresse og opphør for gammel.
        if (Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1 == livshendelse.opplysningstype && Livshendelse.Endringstype.OPPHOERT == livshendelse.endringstype) {
            status = Status.KANSELLERT
        }

        var lagretAktør =
            aktorDao.findByAktorid(livshendelse.aktorid).orElseGet { aktorDao.save(Aktor(livshendelse.aktorid)) }

        return hendelsemottakDao.save(
            Hendelsemottak(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
                livshendelse.endringstype,
                listeMedPersonidenter.joinToString { it },
                lagretAktør,
                livshendelse.opprettet,
                livshendelse.tidligereHendelseid,
                Livshendelse.tilJson(livshendelse),
                livshendelse.master,
                livshendelse.offset,
                status
            )
        )
    }

    @Transactional(readOnly = false)
    fun lagreKontoendring(aktøridKontoeier: String): Kontoendring {
        var aktør = hentEksisterendeEllerOpprettNyAktør(aktøridKontoeier)
        return kontoendringDao.save(Kontoendring(aktør))
    }

    fun henteAktøridTilPersonerMedNyligOppdatertePersonopplysninger(): Set<String> {
        return hendelsemottakDao.aktøridTilPubliseringsklareOverførteHendelser(
            LocalDateTime.now().minusHours(egenskaper.generelt.antallTimerSidenForrigePublisering.toLong())
        )
    }

    fun henteAktøridTilKontoeiereMedNyligeKontoendringer(): Set<String> {
        var mottattFør =
            LocalDateTime.now().minusMinutes(egenskaper.generelt.antallMinutterForsinketVideresending.toLong())
        var publisertFør =
            LocalDateTime.now().minusHours(egenskaper.generelt.antallTimerSidenForrigePublisering.toLong())

        return kontoendringDao.henteKontoeiereForPublisering(mottattFør, publisertFør)
    }

    fun hentEksisterendeEllerOpprettNyAktør(aktørid: String): Aktor {
        var eksisterendeAktør = aktorDao.findByAktorid(aktørid)
        if (eksisterendeAktør.isPresent) {
            return eksisterendeAktør.get()
        } else {
            return aktorDao.save(Aktor(aktørid))
        }
    }

    private fun kansellereTidligereHendelse(livshendelse: Livshendelse): Status {
        var tidligereHendelseMedStatusMottatt =
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

    private fun trekkeTidligereMottatteKontoendringerForPerson(aktør: Aktor) {
        var kontoendringerForPersonMedStatusMottatt =
            kontoendringDao.findByAktorAndStatus(aktør, StatusKontoendring.MOTTATT)
        kontoendringerForPersonMedStatusMottatt.forEach {
            it.status = StatusKontoendring.TRUKKET
            it.statustidspunkt = LocalDateTime.now()
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Livshendelsebehandler::class.java)
    }
}

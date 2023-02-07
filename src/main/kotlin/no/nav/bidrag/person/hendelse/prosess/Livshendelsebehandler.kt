package no.nav.bidrag.person.hendelse.prosess

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Opplysningstype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class Livshendelsebehandler(val databasetjeneste: Databasetjeneste) {
    fun prosesserNyHendelse(livshendelse: Livshendelse) {
        when (livshendelse.opplysningstype) {
            Opplysningstype.ADRESSEBESKYTTELSE_V1 -> behandleAdressebeskyttelse(livshendelse)
            Opplysningstype.BOSTEDSADRESSE_V1 -> behandleBostedsadresse(livshendelse)
            Opplysningstype.DOEDSFALL_V1 -> behandleDødsfall(livshendelse)
            Opplysningstype.FOEDSEL_V1 -> behandleFødsel(livshendelse)
            Opplysningstype.FOLKEREGISTERIDENTIFIKATOR_V1 -> behandleFolkeregisteridentifikator(livshendelse)
            Opplysningstype.INNFLYTTING_TIL_NORGE -> behandleInnflytting(livshendelse)
            Opplysningstype.NAVN_V1 -> behandleNavn(livshendelse)
            Opplysningstype.UTFLYTTING_FRA_NORGE -> behandleUtflytting(livshendelse)
            Opplysningstype.SIVILSTAND_V1 -> behandleSivilstand(livshendelse)
            Opplysningstype.VERGE_V1 -> behandleVerge(livshendelse)
            Opplysningstype.IKKE_STØTTET -> log.error("Forsøk på prosessere medling med opplysningstype som ikke støttes av løsningen.")
        }
    }

    private fun behandleAdressebeskyttelse(livshendelse: Livshendelse) {
        tellerAdressebeskyttelse.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne."
            )
            return
        }

        loggeLivshendelse(livshendelse, "Gradering: ${livshendelse.addressebeskyttelse}")

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerAdressebeskyttelseAnnullert.increment()
            Endringstype.KORRIGERT -> tellerAdressebeskyttelseKorrigert.increment()
            Endringstype.OPPHOERT -> tellerAdressebeskyttelseOpphørt.increment()
            Endringstype.OPPRETTET -> tellerAdressebeskyttelseOpprettet.increment()
        }

        if (Endringstype.OPPRETTET != livshendelse.endringstype) {
            log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Gradering: ${livshendelse.addressebeskyttelse}")
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleVerge(livshendelse: Livshendelse) {

        tellerVerge.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne."
            )
            return
        }

        loggeLivshendelse(livshendelse, "Type: ${livshendelse.verge?.type}, omfang: ${livshendelse.verge?.vergeEllerFullmektig?.omfang}")

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerVergeAnnullert.increment()
            Endringstype.KORRIGERT -> tellerVergeKorrigert.increment()
            Endringstype.OPPHOERT -> tellerVergeOpphørt.increment()
            Endringstype.OPPRETTET -> tellerVergeOpprettet.increment()
        }

        if (Endringstype.OPPRETTET != livshendelse.endringstype) {
            log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Omfang: ${livshendelse.verge?.vergeEllerFullmektig?.omfang}")
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleBostedsadresse(livshendelse: Livshendelse) {

        tellerBostedsadresse.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne."
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerBostedsadresseAnnullert.increment()
            Endringstype.KORRIGERT -> tellerBostedsadresseKorrigert.increment()
            Endringstype.OPPHOERT -> tellerBostedsadresseOpphørt.increment()
            Endringstype.OPPRETTET -> tellerBostedsadresseOpprettet.increment()
        }

        if (Endringstype.OPPRETTET != livshendelse.endringstype) {
            log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Flyttedato: ${livshendelse.flyttedato}")
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleDødsfall(livshendelse: Livshendelse) {

        tellerDødsfall.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne."
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerDødsfallAnnullert.increment()
            Endringstype.KORRIGERT -> tellerDødsfallKorrigert.increment()
            Endringstype.OPPHOERT -> tellerDødsfallOpphørt.increment()
            Endringstype.OPPRETTET -> tellerDødsfallOpprettet.increment()
        }

        loggeLivshendelse(livshendelse, "dødsdato: ${livshendelse.doedsdato}");

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                if (livshendelse.doedsdato == null) {
                    log.error("Mangler dødsdato. Ignorerer hendelse ${livshendelse.hendelseid}")
                    tellerDødsfallIgnorert.increment()
                } else {
                    databasetjeneste.lagreHendelse(livshendelse)
                }
            }

            else -> {
                log.warn("Ignorerer hendelse med id ${livshendelse.hendelseid} og opplysningstype ${livshendelse.opplysningstype}. Dødsdato: ${livshendelse.doedsdato}")
            }
        }
    }

    private fun behandleFolkeregisteridentifikator(livshendelse: Livshendelse) {

        tellerFolkeregisteridentifikator.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne."
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerFolkeregisteridentifikatorAnnullert.increment()
            Endringstype.KORRIGERT -> tellerFolkeregisteridentifikatorKorrigert.increment()
            Endringstype.OPPHOERT -> tellerFolkeregisteridentifikatorOpphørt.increment()
            Endringstype.OPPRETTET -> tellerFolkeregisteridentifikatorOpprettet.increment()
        }

        loggeLivshendelse(livshendelse);

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET -> {
                if (livshendelse.folkeregisteridentifikator?.type == null) {
                    log.error("Mangler folkeregisteridentifikator.type. Ignorerer hendelse ${livshendelse.hendelseid}")
                    tellerFolkeregisteridentifikatorIgnorert.increment()
                }
            }

            else -> {
                log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Endringstype: ${livshendelse.endringstype}")
            }
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleInnflytting(livshendelse: Livshendelse) {

        tellerInnflytting.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.hendelseid}. Ignorerer denne."
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerInnflyttingAnnullert.increment()
            Endringstype.KORRIGERT -> tellerInnflyttingKorrigert.increment()
            Endringstype.OPPHOERT -> tellerInnflyttingOpphørt.increment()
            Endringstype.OPPRETTET -> tellerInnflyttingOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.ANNULLERT -> {
                loggeLivshendelse(livshendelse, "Fraflyttingsland: ${livshendelse.innflytting?.fraflyttingsland}")
                databasetjeneste.lagreHendelse(livshendelse)
            }

            else -> {
                tellerInnflyttingIgnorert.increment()
                loggeLivshendelse(livshendelse, "Ikke av type OPPRETTET eller ANNULLERT.")
            }
        }
    }

    private fun behandleNavn(livshendelse: Livshendelse) {

        tellerNavn.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne."
            )
            return
        }

        loggeLivshendelse(livshendelse);

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerNavnAnnullert.increment()
            Endringstype.KORRIGERT -> tellerNavnKorrigert.increment()
            Endringstype.OPPHOERT -> tellerNavnOpphørt.increment()
            Endringstype.OPPRETTET -> tellerNavnOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                val manglerFornavn = livshendelse.navn?.fornavn == null
                if (manglerFornavn || livshendelse.navn?.etternavn == null) {
                    val navnedel = if (manglerFornavn) "Fornavn" else "Etternavn"
                    log.warn("${navnedel} mangler. Ignorerer navnehendelse med id ${livshendelse.hendelseid}")
                } else {
                    databasetjeneste.lagreHendelse(livshendelse)
                }
            }

            Endringstype.ANNULLERT -> {
                databasetjeneste.lagreHendelse(livshendelse)
            }

            else -> {
                log.warn("Ignorerer navnehendelse med id ${livshendelse.hendelseid} av type ${livshendelse.opplysningstype}. Endringstype: ${livshendelse.endringstype}")
            }
        }
    }

    private fun behandleFødsel(livshendelse: Livshendelse) {

        tellerFødsel.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne."
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerFødselAnnulert.increment()
            Endringstype.KORRIGERT -> tellerFødselKorrigert.increment()
            Endringstype.OPPHOERT -> tellerFødselOpphørt.increment()
            Endringstype.OPPRETTET -> tellerFødselOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                loggeLivshendelse(livshendelse, "fødselsdato: ${livshendelse.fødsel?.fødselsdato}")
                val fødselsdato = livshendelse.fødsel?.fødselsdato
                if (fødselsdato == null) {
                    tellerFødselIgnorert.increment()
                    log.warn("Mangler fødselsdato. Ignorerer hendelse ${livshendelse.hendelseid}")
                } else if (erUnder6mnd(fødselsdato)) {
                    tellerFødselIgnorert.increment()
                    if (erUtenforNorge(livshendelse.fødsel.fødeland)) {
                        log.info("Fødeland er ikke Norge. Ignorerer hendelse ${livshendelse.hendelseid}")
                    } else {
                        databasetjeneste.lagreHendelse(livshendelse)
                    }
                }
            }

            Endringstype.ANNULLERT -> {
                if (livshendelse.tidligereHendelseid == null) {
                    log.warn("Mottatt annullert fødsel uten tidligereHendelseId, hendelseId ${livshendelse.hendelseid}")
                } else {
                    databasetjeneste.lagreHendelse(livshendelse)
                }
            }

            else -> {
                loggeLivshendelse(livshendelse)
            }
        }
    }

    private fun behandleUtflytting(livshendelse: Livshendelse) {

        tellerUtflytting.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne."
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerUtflyttingAnnullert.increment()
            Endringstype.KORRIGERT -> tellerUtflyttingKorrigert.increment()
            Endringstype.OPPHOERT -> tellerUtflyttingOpphørt.increment()
            Endringstype.OPPRETTET -> tellerUtflyttingOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.ANNULLERT -> {
                loggeLivshendelse(livshendelse, "utflyttingsdato: ${livshendelse.utflytting?.utflyttingsdato}")
                databasetjeneste.lagreHendelse(livshendelse)
            }

            else -> {
                tellerUtflyttingIgnorert.increment()
                loggeLivshendelse(livshendelse, "Ikke av type OPPRETTET eller ANNULLERT.")
            }
        }
    }

    private fun behandleSivilstand(livshendelse: Livshendelse) {

        tellerSivilstand.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            log.info(
                "Mottok duplikat livshendelse (hendelseid: ${livshendelse.hendelseid}) med opplysningstype ${livshendelse.opplysningstype}. Ignorerer denne."
            )
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerSivilstandAnnullert.increment()
            Endringstype.KORRIGERT -> tellerSivilstandKorrigert.increment()
            Endringstype.OPPHOERT -> tellerSivilstandOpphørt.increment()
            Endringstype.OPPRETTET -> tellerSivilstandOpprettet.increment()
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT, Endringstype.ANNULLERT -> {
                loggeLivshendelse(livshendelse, "sivilstandDato: ${livshendelse.sivilstand?.bekreftelsesdato}")
                databasetjeneste.lagreHendelse(livshendelse)
            }

            else -> {
                loggeLivshendelse(livshendelse, "Ikke av type OPPRETTET, KORRIGERT, eller ANNULLERT.")
            }
        }
    }

    private fun loggeLivshendelse(livshendelse: Livshendelse, ekstraInfo: String = "") {
        log.info(
            "Livshendelse mottatt: " +
                    "hendelseId: ${livshendelse.hendelseid} " +
                    "offset: ${livshendelse.offset}, " +
                    "opplysningstype: ${livshendelse.opplysningstype}, " +
                    "aktørid: ${livshendelse.hentGjeldendeAktørid()}, " +
                    "endringstype: ${livshendelse.endringstype}, $ekstraInfo"
        )
    }

    private fun erUnder6mnd(fødselsDato: LocalDate): Boolean {
        return LocalDate.now().isBefore(fødselsDato.plusMonths(6))
    }

    private fun erUtenforNorge(fødeland: String?): Boolean {
        return when (fødeland) {
            null, "NOR" -> false
            else -> true
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Livshendelsebehandler::class.java)
        const val MAKS_ANTALL_PERSONIDENTER = 20

        val tellerLeesahDuplikat: Counter = Metrics.counter(tellernavn("leesah.duplikat"))

        const val adressebeskyttelse = "adressebeskyttelse"
        val tellerAdressebeskyttelse: Counter = Metrics.counter(tellernavn(adressebeskyttelse))
        val tellerAdressebeskyttelseAnnullert: Counter = Metrics.counter(tellernavn(adressebeskyttelse + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerAdressebeskyttelseKorrigert: Counter = Metrics.counter(tellernavn(adressebeskyttelse + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerAdressebeskyttelseOpphørt: Counter = Metrics.counter(tellernavn(adressebeskyttelse + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerAdressebeskyttelseOpprettet: Counter = Metrics.counter(tellernavn(adressebeskyttelse + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val bostedsadresse = "bostedsadresse"
        val tellerBostedsadresse: Counter = Metrics.counter(tellernavn(bostedsadresse))
        val tellerBostedsadresseAnnullert: Counter = Metrics.counter(tellernavn(bostedsadresse + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerBostedsadresseKorrigert: Counter = Metrics.counter(tellernavn(bostedsadresse+ ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerBostedsadresseOpphørt: Counter = Metrics.counter(tellernavn(bostedsadresse+ ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerBostedsadresseOpprettet: Counter = Metrics.counter(tellernavn(bostedsadresse+ ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val dødsfall = "doedsfall"
        val tellerDødsfall: Counter = Metrics.counter(tellernavn(dødsfall))
        val tellerDødsfallAnnullert: Counter = Metrics.counter(tellernavn(dødsfall + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerDødsfallKorrigert: Counter = Metrics.counter(tellernavn(dødsfall + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerDødsfallOpphørt: Counter = Metrics.counter(tellernavn(dødsfall + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerDødsfallOpprettet: Counter = Metrics.counter(tellernavn(dødsfall + ".${Endringstype.OPPRETTET.name.lowercase()}"))
        val tellerDødsfallIgnorert: Counter = Metrics.counter(tellernavn(dødsfall + ".ignorert"))

        const val folkeregisteridentifikator = "folkeregisteridentifikator"
        val tellerFolkeregisteridentifikator: Counter = Metrics.counter(tellernavn(folkeregisteridentifikator))
        val tellerFolkeregisteridentifikatorAnnullert: Counter = Metrics.counter(tellernavn(folkeregisteridentifikator + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerFolkeregisteridentifikatorKorrigert: Counter = Metrics.counter(tellernavn(folkeregisteridentifikator + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerFolkeregisteridentifikatorOpphørt: Counter = Metrics.counter(tellernavn(folkeregisteridentifikator + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerFolkeregisteridentifikatorOpprettet: Counter = Metrics.counter(tellernavn(folkeregisteridentifikator + ".${Endringstype.OPPRETTET.name.lowercase()}"))
        val tellerFolkeregisteridentifikatorIgnorert: Counter = Metrics.counter(tellernavn(folkeregisteridentifikator + ".ignorert"))

        const val fødsel = "foedsel"
        val tellerFødsel: Counter = Metrics.counter(tellernavn(fødsel))
        val tellerFødselIgnorert: Counter = Metrics.counter(tellernavn(fødsel + ".ignorert"))
        val tellerFødselAnnulert: Counter = Metrics.counter(tellernavn(fødsel + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerFødselKorrigert: Counter = Metrics.counter(tellernavn(fødsel + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerFødselOpphørt: Counter = Metrics.counter(tellernavn(fødsel + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerFødselOpprettet: Counter = Metrics.counter(tellernavn(fødsel + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val innflytting = "innflytting"
        val tellerInnflytting: Counter = Metrics.counter(tellernavn(innflytting))
        val tellerInnflyttingAnnullert: Counter = Metrics.counter(tellernavn(innflytting + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerInnflyttingKorrigert: Counter = Metrics.counter(tellernavn(innflytting + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerInnflyttingOpphørt: Counter = Metrics.counter(tellernavn(innflytting + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerInnflyttingOpprettet: Counter = Metrics.counter(tellernavn(innflytting + ".${Endringstype.OPPRETTET.name.lowercase()}"))
        val tellerInnflyttingIgnorert: Counter = Metrics.counter(tellernavn((innflytting + ".ignorert")))

        const val navn = "navn"
        val tellerNavn: Counter = Metrics.counter(tellernavn(navn))
        val tellerNavnAnnullert: Counter = Metrics.counter(tellernavn(navn + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerNavnKorrigert: Counter = Metrics.counter(tellernavn(navn + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerNavnOpphørt: Counter = Metrics.counter(tellernavn(navn + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerNavnOpprettet: Counter = Metrics.counter(tellernavn(navn + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val sivilstand = "sivilstand"
        val tellerSivilstand: Counter = Metrics.counter(tellernavn(sivilstand))
        val tellerSivilstandAnnullert: Counter = Metrics.counter(tellernavn(sivilstand + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerSivilstandKorrigert: Counter = Metrics.counter(tellernavn(sivilstand + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerSivilstandOpphørt: Counter = Metrics.counter(tellernavn(sivilstand + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerSivilstandOpprettet: Counter = Metrics.counter(tellernavn(sivilstand + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        const val utflytting = "utflytting"
        val tellerUtflytting: Counter = Metrics.counter(tellernavn(utflytting))
        val tellerUtflyttingAnnullert: Counter = Metrics.counter(tellernavn(utflytting + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerUtflyttingKorrigert: Counter = Metrics.counter(tellernavn(utflytting + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerUtflyttingOpphørt: Counter = Metrics.counter(tellernavn(utflytting + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerUtflyttingOpprettet: Counter = Metrics.counter(tellernavn(utflytting + ".${Endringstype.OPPRETTET.name.lowercase()}"))
        val tellerUtflyttingIgnorert: Counter = Metrics.counter(tellernavn(utflytting + ".ignorert"))

        const val verge = "verge"
        val tellerVerge: Counter = Metrics.counter(tellernavn(verge))
        val tellerVergeAnnullert: Counter = Metrics.counter(tellernavn(verge + ".${Endringstype.ANNULLERT.name.lowercase()}"))
        val tellerVergeKorrigert: Counter = Metrics.counter(tellernavn(verge + ".${Endringstype.KORRIGERT.name.lowercase()}"))
        val tellerVergeOpphørt: Counter = Metrics.counter(tellernavn(verge + ".${Endringstype.OPPHOERT.name.lowercase()}"))
        val tellerVergeOpprettet: Counter = Metrics.counter(tellernavn(verge + ".${Endringstype.OPPRETTET.name.lowercase()}"))

        private fun tellernavn(navn: String): String {
            return "bidrag.personhendelse.$navn"
        }
    }
}

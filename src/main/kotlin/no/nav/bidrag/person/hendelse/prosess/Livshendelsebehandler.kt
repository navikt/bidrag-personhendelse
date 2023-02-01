package no.nav.bidrag.person.hendelse.prosess

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse.Opplysningstype
import no.nav.bidrag.person.hendelse.integrasjon.distribusjon.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Wmq
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class Livshendelsebehandler(
    val egenskaperWmq: Wmq,
    val databasetjeneste: Databasetjeneste,
    val meldingsprodusent: Meldingsprodusent
) {
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
        }
    }

    private fun behandleAdressebeskyttelse(livshendelse: Livshendelse) {
        tellerAdressebeskyttelse.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
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

        meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, Livshendelse.tilJson(livshendelse))
        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleVerge(livshendelse: Livshendelse) {

        tellerVerge.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
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

        meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, Livshendelse.tilJson(livshendelse))
        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleBostedsadresse(livshendelse: Livshendelse) {

        tellerBostedsadresse.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.ANNULLERT -> tellerBostedsadresseAnnullert.increment()
            Endringstype.KORRIGERT -> tellerBostedsadresseKorrigert.increment()
            Endringstype.OPPHOERT -> tellerBostedsadresseOpphørt.increment()
            else -> {}
        }

        if (!Endringstype.OPPRETTET.equals(livshendelse.endringstype)) {
            log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Flyttedato: ${livshendelse.flyttedato}")
        }

        meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, Livshendelse.tilJson(livshendelse))
        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleDødsfall(livshendelse: Livshendelse) {
        tellerDødsfall.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        loggeLivshendelse(livshendelse, "dødsdato: ${livshendelse.doedsdato}");

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET -> {
                if (livshendelse.doedsdato == null) {
                    log.error("Mangler dødsdato. Ignorerer hendelse ${livshendelse.hendelseid}")
                    tellerDødsfallIgnorert.increment()
                } else {
                    tellerDødsfall.increment()
                    var melding = Livshendelse.tilJson(livshendelse)
                    meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, melding)
                }
            }

            else -> {
                log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Dødsdato: ${livshendelse.doedsdato}")
            }
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleFolkeregisteridentifikator(livshendelse: Livshendelse) {

        tellerFolkeregisteridentifikator.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        loggeLivshendelse(livshendelse);

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET -> {
                if (livshendelse.folkeregisteridentifikator?.type == null) {
                    log.error("Mangler folkeregisteridentifikator.type. Ignorerer hendelse ${livshendelse.hendelseid}")
                    tellerFolkeregisteridentifikatorIgnorert.increment()
                } else {
                    meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, Livshendelse.tilJson(livshendelse))
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
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET -> {
                loggeLivshendelse(livshendelse, "Fraflyttingsland: ${livshendelse.innflytting?.fraflyttingsland}")
                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, Livshendelse.tilJson(livshendelse))
            }

            else -> {
                tellerInnflyttingIgnorert.increment()
                loggeLivshendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleNavn(livshendelse: Livshendelse) {
        tellerNavn.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        loggeLivshendelse(livshendelse);

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET -> {
                var manglerFornavn = livshendelse.navn?.fornavn == null
                if (manglerFornavn || livshendelse.navn?.etternavn == null) {
                    var navnedel = if (manglerFornavn) "Fornavn" else "Etternavn"
                    log.info("${navnedel} mangler. Ignorerer navnehendelse med id ${livshendelse.hendelseid}")
                } else {
                    meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, Livshendelse.tilJson(livshendelse))
                }
            }

            else -> {
                log.warn("Navnehendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Endringstype: ${livshendelse.endringstype}")
            }
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleFødsel(livshendelse: Livshendelse) {
        tellerFødsel.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
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
                        meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, Livshendelse.tilJson(livshendelse))
                    }
                }
            }

            Endringstype.ANNULLERT -> {
                tellerFødselAnnulert.increment()
                if (livshendelse.tidligereHendelseid == null) {
                    log.warn("Mottatt annullert fødsel uten tidligereHendelseId, hendelseId ${livshendelse.hendelseid}")
                } else {
                    meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, Livshendelse.tilJson(livshendelse))
                }
            }

            else -> {
                loggeLivshendelse(livshendelse)
            }
        }
        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleUtflytting(livshendelse: Livshendelse) {

        tellerUtflytting.increment()

        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET -> {
                loggeLivshendelse(livshendelse, "utflyttingsdato: ${livshendelse.utflytting?.utflyttingsdato}")
                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, Livshendelse.tilJson(livshendelse))
            }

            else -> {
                tellerUtflyttingIgnorert.increment()
                loggeLivshendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }

        databasetjeneste.lagreHendelse(livshendelse)
    }

    private fun behandleSivilstand(livshendelse: Livshendelse) {

        tellerSivilstand.increment()
        if (databasetjeneste.hendelseFinnesIDatabasen(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        when (livshendelse.endringstype) {
            Endringstype.OPPRETTET -> {
                loggeLivshendelse(livshendelse, "sivilstandDato: ${livshendelse.sivilstand?.bekreftelsesdato}")
                var livshendelseJson = Livshendelse.tilJson(livshendelse)
                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, livshendelseJson)
            }

            else -> {
                loggeLivshendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }

        databasetjeneste.lagreHendelse(livshendelse)
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

        val tellerAdressebeskyttelse: Counter = Metrics.counter(tellernavn("adressebeskyttelse"))
        val tellerAdressebeskyttelseAnnullert: Counter = Metrics.counter(tellernavn("adressebeskyttelse.annullert"))
        val tellerAdressebeskyttelseKorrigert: Counter = Metrics.counter(tellernavn("adressebeskyttelse.korrigert"))
        val tellerAdressebeskyttelseOpphørt: Counter = Metrics.counter(tellernavn("adressebeskyttelse.opphørt"))
        val tellerAdressebeskyttelseOpprettet: Counter = Metrics.counter(tellernavn("adressebeskyttelse.opprettet"))
        val tellerBostedsadresse: Counter = Metrics.counter(tellernavn("bostedsadresse"))
        val tellerBostedsadresseKorrigert: Counter = Metrics.counter(tellernavn("bostedsadresse.korrigert"))
        val tellerBostedsadresseAnnullert: Counter = Metrics.counter(tellernavn("bostedsadresse.annullert"))
        val tellerBostedsadresseOpphørt: Counter = Metrics.counter(tellernavn("bostedsadresse.opphørt"))
        val tellerBostedsadresseOpprettet: Counter = Metrics.counter(tellernavn("bostedsadresse.opprettet"))
        val tellerDødsfall: Counter = Metrics.counter(tellernavn("dødsfall"))
        val tellerDødsfallIgnorert: Counter = Metrics.counter(tellernavn("dødsfall.ignorert"))
        val tellerFolkeregisteridentifikator: Counter = Metrics.counter(tellernavn("folkeregisteridentifikator"))
        val tellerFolkeregisteridentifikatorIgnorert: Counter = Metrics.counter(tellernavn("folkeregisteridentifikator.ignorert"))
        val tellerFødsel: Counter = Metrics.counter(tellernavn("fødsel"))
        val tellerFødselIgnorert: Counter = Metrics.counter(tellernavn("fødsel.ignorert"))
        val tellerFødselAnnulert: Counter = Metrics.counter(tellernavn("fødsel.annullert"))
        val tellerInnflytting: Counter = Metrics.counter(tellernavn(("innflytting")))
        val tellerInnflyttingIgnorert: Counter = Metrics.counter(tellernavn(("innflytting.ignorert")))
        val tellerNavn: Counter = Metrics.counter(tellernavn("navn"))
        val tellerSivilstand: Counter = Metrics.counter(tellernavn("sivilstand"))
        val tellerUtflytting: Counter = Metrics.counter(tellernavn("utflytting"))
        val tellerUtflyttingIgnorert: Counter = Metrics.counter(tellernavn("utflytting.ignorert"))
        val tellerLeesahDuplikat: Counter = Metrics.counter(tellernavn("leesah.duplikat"))
        val tellerVerge: Counter = Metrics.counter(tellernavn("verge"))
        val tellerVergeAnnullert: Counter = Metrics.counter(tellernavn("verge.annullert"))
        val tellerVergeKorrigert: Counter = Metrics.counter(tellernavn("verge.korrigert"))
        val tellerVergeOpphørt: Counter = Metrics.counter(tellernavn("verge.opphørt"))
        val tellerVergeOpprettet: Counter = Metrics.counter(tellernavn("verge.opprettet"))

        fun tellernavn(navn: String): String {
            return "bidrag.personhendelse.$navn"
        }
    }
}

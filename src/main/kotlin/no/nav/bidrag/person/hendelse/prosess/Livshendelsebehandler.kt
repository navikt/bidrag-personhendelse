package no.nav.bidrag.person.hendelse.prosess

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.database.Hendelsearkiv
import no.nav.bidrag.person.hendelse.database.HendelsearkivDao
import no.nav.bidrag.person.hendelse.integrasjon.distribusjon.Meldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Wmq
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class Livshendelsebehandler(
    val egenskaperWmq: Wmq,
    val hendelsearkivDao: HendelsearkivDao,
    val meldingsprodusent: Meldingsprodusent
) {
    fun prosesserNyHendelse(livshendelse: Livshendelse) {
        when (livshendelse.opplysningstype) {
            Opplysningstype.ADRESSEBESKYTTELSE_V1.toString() -> behandleAdressebeskyttelse(livshendelse)
            Opplysningstype.BOSTEDSADRESSE_V1.toString() -> behandleBostedsadresse(livshendelse)
            Opplysningstype.DOEDSFALL_V1.toString() -> behandleDødsfall(livshendelse)
            Opplysningstype.FOEDSEL_V1.toString() -> behandleFødsel(livshendelse)
            Opplysningstype.FOLKEREGISTERIDENTIFIKATOR_V1.toString() -> behandleFolkeregisteridentifikator(livshendelse)
            Opplysningstype.INNFLYTTING_V1.toString() -> behandleInnflytting(livshendelse)
            Opplysningstype.NAVN_V1.toString() -> behandleNavn(livshendelse)
            Opplysningstype.UTFLYTTING_V1.toString() -> behandleUtflytting(livshendelse)
            Opplysningstype.SIVILSTAND_V1.toString() -> behandleSivilstand(livshendelse)
            Opplysningstype.VERGE_V1.toString() -> behandleVerge(livshendelse)
        }
    }

    private fun behandleAdressebeskyttelse(livshendelse: Livshendelse) {
        tellerAdressebeskyttelse.increment()

        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        loggeLivshendelse(livshendelse, "Gradering: ${livshendelse.addressebeskyttelse}")

        when (livshendelse.endringstype) {
            ANNULLERT -> tellerVergeAnnullert.increment()
            KORRIGERT -> tellerVergeKorrigert.increment()
            OPPHOERT -> tellerVergeOpphørt.increment()
        }

        if (!OPPRETTET.equals(livshendelse.endringstype)) {
            log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Gradering: ${livshendelse.addressebeskyttelse}")
        }

        meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
        arkivereHendelse(livshendelse)
    }

    private fun behandleVerge(livshendelse: Livshendelse) {

        tellerVerge.increment()

        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        loggeLivshendelse(livshendelse, "Type: ${livshendelse.verge?.type}, omfang: ${livshendelse.verge?.vergeEllerFullmektig?.omfang}")

        when (livshendelse.endringstype) {
            ANNULLERT -> tellerVergeAnnullert.increment()
            KORRIGERT -> tellerVergeKorrigert.increment()
            OPPHOERT -> tellerVergeOpphørt.increment()
        }

        if (!OPPRETTET.equals(livshendelse.endringstype)) {
            log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Omfang: ${livshendelse.verge?.vergeEllerFullmektig?.omfang}")
        }

        meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
        arkivereHendelse(livshendelse)
    }

    private fun behandleBostedsadresse(livshendelse: Livshendelse) {

        tellerBostedsadresse.increment()

        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        when (livshendelse.endringstype) {
            ANNULLERT -> tellerBostedsadresseAnnullert.increment()
            KORRIGERT -> tellerBostedsadresseKorrigert.increment()
            OPPHOERT -> tellerBostedsadresseOpphørt.increment()
        }

        if (!OPPRETTET.equals(livshendelse.endringstype)) {
            log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Flyttedato: ${livshendelse.flyttedato}")
        }

        meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
        arkivereHendelse(livshendelse)
    }

    private fun behandleDødsfall(livshendelse: Livshendelse) {
        tellerDødsfall.increment()

        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        loggeLivshendelse(livshendelse, "dødsdato: ${livshendelse.doedsdato}");

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                if (livshendelse.doedsdato == null) {
                    log.error("Mangler dødsdato. Ignorerer hendelse ${livshendelse.hendelseid}")
                    tellerDødsfallIgnorert.increment()
                } else {
                    tellerDødsfall.increment()
                    var melding = oppretteGson().toJson(livshendelse)
                    meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, melding)
                }
            }

            else -> {
                log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Dødsdato: ${livshendelse.doedsdato}")
            }
        }

        arkivereHendelse(livshendelse)
    }

    private fun behandleFolkeregisteridentifikator(livshendelse: Livshendelse) {

        tellerFolkeregisteridentifikator.increment()

        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        loggeLivshendelse(livshendelse);

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                if (livshendelse.folkeregisteridentifikator?.type == null) {
                    log.error("Mangler folkeregisteridentifikator.type. Ignorerer hendelse ${livshendelse.hendelseid}")
                    tellerFolkeregisteridentifikatorIgnorert.increment()
                } else {
                    meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
                }
            }

            else -> {
                log.warn("Hendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Endringstype: ${livshendelse.endringstype}")
            }
        }

        arkivereHendelse(livshendelse)
    }

    private fun behandleInnflytting(livshendelse: Livshendelse) {
        tellerInnflytting.increment()
        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                loggeLivshendelse(livshendelse, "Fraflyttingsland: ${livshendelse.innflytting?.fraflyttingsland}")
                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
            }

            else -> {
                tellerInnflyttingIgnorert.increment()
                loggeLivshendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }

        arkivereHendelse(livshendelse)
    }

    private fun behandleNavn(livshendelse: Livshendelse) {
        tellerNavn.increment()

        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        loggeLivshendelse(livshendelse);

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                var manglerFornavn = livshendelse.navn?.fornavn == null
                if (manglerFornavn || livshendelse.navn?.etternavn == null) {
                    var navnedel = if (manglerFornavn) "Fornavn" else "Etternavn"
                    log.info("${navnedel} mangler. Ignorerer navnehendelse med id ${livshendelse.hendelseid}")
                } else {
                    meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
                }
            }

            else -> {
                log.warn("Navnehendelse med id ${livshendelse.hendelseid} var ikke type OPPRETTET. Endringstype: ${livshendelse.endringstype}")
            }
        }

        arkivereHendelse(livshendelse)
    }

    private fun behandleFødsel(livshendelse: Livshendelse) {
        tellerFødsel.increment()

        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        when (livshendelse.endringstype) {
            OPPRETTET, KORRIGERT -> {
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
                        meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
                    }
                }
            }

            ANNULLERT -> {
                tellerFødselAnnulert.increment()
                if (livshendelse.tidligereHendelseid == null) {
                    log.warn("Mottatt annullert fødsel uten tidligereHendelseId, hendelseId ${livshendelse.hendelseid}")
                } else {
                    meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
                }
            }

            else -> {
                loggeLivshendelse(livshendelse)
            }
        }
        arkivereHendelse(livshendelse)
    }

    private fun behandleUtflytting(livshendelse: Livshendelse) {

        tellerUtflytting.increment()

        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                loggeLivshendelse(livshendelse, "utflyttingsdato: ${livshendelse.utflytting?.utflyttingsdato}")
                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, oppretteGson().toJson(livshendelse))
            }

            else -> {
                tellerUtflyttingIgnorert.increment()
                loggeLivshendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }

        arkivereHendelse(livshendelse)
    }

    private fun behandleSivilstand(livshendelse: Livshendelse) {

        tellerSivilstand.increment()
        if (hendelsearkivDao.existsByHendelseidAndOpplysningstype(livshendelse.hendelseid, livshendelse.opplysningstype)) {
            tellerLeesahDuplikat.increment()
            return
        }

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                loggeLivshendelse(livshendelse, "sivilstandDato: ${livshendelse.sivilstand?.sivilstandDato}")
                var livshendelseJson = oppretteGson().toJson(livshendelse)
                meldingsprodusent.sendeMelding(egenskaperWmq.queueNameLivshendelser, livshendelseJson)
            }

            else -> {
                loggeLivshendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }

        arkivereHendelse(livshendelse)
    }

    private fun arkivereHendelse(livshendelse: Livshendelse) {

        var listeMedPersonidenter = livshendelse.personidenter

        if (livshendelse.personidenter?.size!! > MAKS_ANTALL_PERSONIDENTER) {
            listeMedPersonidenter = listeMedPersonidenter?.subList(0, MAKS_ANTALL_PERSONIDENTER)
            log.warn(
                "Mottatt livshendelse med hendelseid ${livshendelse.hendelseid} inneholdt over ${MAKS_ANTALL_PERSONIDENTER} personidenter. " +
                        "Kun de ${MAKS_ANTALL_PERSONIDENTER} første arkiveres."
            )
        }

        hendelsearkivDao.save(
            Hendelsearkiv(
                livshendelse.hendelseid,
                livshendelse.opplysningstype,
                livshendelse.endringstype,
                livshendelse.master,
                livshendelse.offset,
                listeMedPersonidenter?.joinToString { it },
                livshendelse.tidligereHendelseid,
                oppretteGson().toJson(livshendelse)
            )
        )
    }

    private fun oppretteGson(): Gson {
        var gsonbuilder = GsonBuilder()
        gsonbuilder.registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter().nullSafe())
        gsonbuilder.registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter().nullSafe())
        var gson = gsonbuilder.create()
        return gson
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
        const val OPPRETTET = "OPPRETTET"
        const val KORRIGERT = "KORRIGERT"
        const val ANNULLERT = "ANNULLERT"
        const val OPPHOERT = "OPPHOERT"
        const val MAKS_ANTALL_PERSONIDENTER = 20

        val tellerAdressebeskyttelse: Counter = Metrics.counter(tellernavn("adressebeskyttelse"))
        val tellerAdressebeskyttelseAnnullert: Counter = Metrics.counter(tellernavn("adressebeskyttelse.annullert"))
        val tellerAdressebeskyttelseKorrigert: Counter = Metrics.counter(tellernavn("adressebeskyttelse.korrigert"))
        val tellerAdressebeskyttelseOpphørt: Counter = Metrics.counter(tellernavn("adressebeskyttelse.opphørt"))
        val tellerBostedsadresse: Counter = Metrics.counter(tellernavn("bostedsadresse"))
        val tellerBostedsadresseKorrigert: Counter = Metrics.counter(tellernavn("bostedsadresse.korrigert"))
        val tellerBostedsadresseAnnullert: Counter = Metrics.counter(tellernavn("bostedsadresse.annullert"))
        val tellerBostedsadresseOpphørt: Counter = Metrics.counter(tellernavn("bostedsadresse.opphørt"))
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

        fun tellernavn(navn: String): String {
            return "bidrag.personhendelse.$navn"
        }
    }

    enum class Opplysningstype {
        ADRESSEBESKYTTELSE_V1, BOSTEDSADRESSE_V1, DOEDSFALL_V1, FOEDSEL_V1, FOLKEREGISTERIDENTIFIKATOR_V1, INNFLYTTING_V1, NAVN_V1, UTFLYTTING_V1, SIVILSTAND_V1, VERGE_V1
    }

}

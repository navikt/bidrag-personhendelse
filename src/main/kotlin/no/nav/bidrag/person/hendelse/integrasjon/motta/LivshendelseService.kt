package no.nav.bidrag.person.hendelse.integrasjon.motta

import no.nav.bidrag.person.hendelse.domene.Livshendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LivshendelseService {

    fun prosesserNyHendelse(livshendelse: Livshendelse) {
        when (livshendelse.opplysningstype) {
            OPPLYSNINGSTYPE_DØDSFALL -> behandleDødsfallHendelse(livshendelse)
            OPPLYSNINGSTYPE_FØDSEL -> behandleFødselsHendelse(livshendelse)
            OPPLYSNINGSTYPE_UTFLYTTING -> behandleUtflyttingHendelse(livshendelse)
            OPPLYSNINGSTYPE_SIVILSTAND -> behandleSivilstandHendelse(livshendelse)
        }
    }

    private fun behandleDødsfallHendelse(livshendelse: Livshendelse) {

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                if (livshendelse.dødsdato == null) {
                    log.error("Mangler dødsdato. Ignorerer hendelse ${livshendelse.hendelseId}")
                }
            }
            else -> {
                logHendelse(livshendelse)
                logHendelse(livshendelse, "Ikke av type OPPRETTET. Dødsdato: ${livshendelse.dødsdato}")
            }
        }
    }

    private fun behandleFødselsHendelse(livshendelse: Livshendelse) {
        when (livshendelse.endringstype) {
            OPPRETTET, KORRIGERT -> {
                logHendelse(livshendelse, "fødselsdato: ${livshendelse.fødselsdato}")
                val fødselsdato = livshendelse.fødselsdato
                if (fødselsdato == null) {
                    log.warn("Mangler fødselsdato. Ignorerer hendelse ${livshendelse.hendelseId}")
                } else if (erUnder6mnd(fødselsdato)) {
                    if (erUtenforNorge(livshendelse.fødeland)) {
                        log.info("Fødeland er ikke Norge. Ignorerer hendelse ${livshendelse.hendelseId}")
                    }
                }
            }
            ANNULLERT -> {
                if (livshendelse.tidligereHendelseId == null) {
                    log.warn("Mottatt annuller fødsel uten tidligereHendelseId, hendelseId ${livshendelse.hendelseId}")
                }
            }
            else -> {
                logHendelse(livshendelse)
            }
        }
    }

    private fun behandleUtflyttingHendelse(livshendelse: Livshendelse) {

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                logHendelse(livshendelse, "utflyttingsdato: ${livshendelse.utflyttingsdato}")
            }
            else -> {
                logHendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }
    }

    private fun behandleSivilstandHendelse(livshendelse: Livshendelse) {

        when (livshendelse.endringstype) {
            OPPRETTET -> {
                logHendelse(livshendelse, "sivilstandDato: ${livshendelse.sivilstandDato}")
            }
            else -> {
                logHendelse(livshendelse, "Ikke av type OPPRETTET.")
            }
        }
    }

    private fun logHendelse(livshendelse: Livshendelse, ekstraInfo: String = "") {
        log.info(
            "person-pdl-leesah melding mottatt: " +
                "hendelseId: ${livshendelse.hendelseId} " +
                "offset: ${livshendelse.offset}, " +
                "opplysningstype: ${livshendelse.opplysningstype}, " +
                "aktørid: ${livshendelse.gjeldendeAktørId}, " +
                "endringstype: ${livshendelse.endringstype}, $ekstraInfo"
        )
    }

    private fun erUnder18år(fødselsDato: LocalDate): Boolean {
        return LocalDate.now().isBefore(fødselsDato.plusYears(18))
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
        val log: Logger = LoggerFactory.getLogger(LivshendelseService::class.java)
        const val OPPRETTET = "OPPRETTET"
        const val KORRIGERT = "KORRIGERT"
        const val ANNULLERT = "ANNULLERT"
        const val OPPLYSNINGSTYPE_DØDSFALL = "DOEDSFALL_V1"
        const val OPPLYSNINGSTYPE_FØDSEL = "FOEDSEL_V1"
        const val OPPLYSNINGSTYPE_UTFLYTTING = "UTFLYTTING_FRA_NORGE"
        const val OPPLYSNINGSTYPE_SIVILSTAND = "SIVILSTAND_V1"
    }
}

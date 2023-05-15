package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import java.time.LocalDateTime

annotation class NoArg

@NoArg
abstract class Personhendelse(
    open var personidenter: String,
    open var aktørid: String
)

@Entity
class Hendelsemottak(
    val hendelseid: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "opplysningstype", nullable = false, updatable = false)
    val opplysningstype: Livshendelse.Opplysningstype,
    @Enumerated(EnumType.STRING)
    val endringstype: Livshendelse.Endringstype,
    @Column(name = "personidenter", nullable = false)
    val personidenter: String,
    @Column(name = "aktorid", nullable = false)
    val aktorid: String,
    @Column(name = "opprettet", nullable = false, updatable = false)
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val tidligereHendelseid: String? = null,
    @Column(length = 5000)
    val hendelse: String = "",
    val master: String = "",
    val offset_pdl: Long = 0L,
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    var status: Status = Status.MOTTATT,
    var statustidspunkt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
)

enum class Status {
    MOTTATT,
    KANSELLERT,
    OVERFØRT,
    OVERFØRING_FEILET,
    PUBLISERT
}

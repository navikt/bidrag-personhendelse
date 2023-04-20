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

@Entity
class Hendelsemottak(
    val hendelseid: String = "",
    @Enumerated(EnumType.STRING)
    val opplysningstype: Livshendelse.Opplysningstype? = null,
    @Enumerated(EnumType.STRING)
    val endringstype: Livshendelse.Endringstype? = null,
    @Column(name = "opprettet", nullable = false, updatable = false)
    val opprettet: LocalDateTime = LocalDateTime.now(),
    @Column(name = "personidenter", nullable = true)
    val personidenter: String? = null,
    @Column(name = "aktorid")
    val aktorid: String? = null,
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

@Entity
@Table(indexes = [Index(name = "index_kontoendring_kontoeier", columnList = "kontoeier", unique = false)])
class Kontoendring(
    @Column(name = "kontoeier", nullable = false)
    val kontoeier: String = "",
    @Column(name = "mottatt", nullable = false)
    val mottatt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "publisert", nullable = true)
    val publisert: LocalDateTime? = null,
    @Column(name = "status", nullable = false)
    var status: StatusKontoendring = StatusKontoendring.MOTTATT,
    @Column(name = "statustidspunkt", nullable = false)
    var statustidspunkt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
)

@Entity
class BidragPersonhendelse(
    @Column(nullable= false)
    val aktorid: String,
    @Column
    val publisert: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = 0
)

enum class Status {
    MOTTATT,
    KANSELLERT,
    OVERFØRT,
    OVERFØRING_FEILET,
    PUBLISERT,
    PUBLISERING_FEILET
}

enum class StatusKontoendring {
    MOTTATT,
    TRUKKET,
    PUBLISERT,
    PUBLISERING_FEILET
}

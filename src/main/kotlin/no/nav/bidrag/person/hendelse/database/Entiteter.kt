package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import java.time.LocalDateTime

annotation class NoArg

@NoArg
abstract class Personhendelse(
    open var personidenter: String,
    open var aktor: Aktor
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
    override var personidenter: String,
    @ManyToOne(cascade = arrayOf(CascadeType.MERGE))
    override var aktor: Aktor,
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
) : Personhendelse(personidenter, aktor)

@Entity
@Table(indexes = [Index(name = "index_kontoendring_aktor_id", columnList = "aktor_id", unique = false)])
class Kontoendring(
    @ManyToOne(cascade = arrayOf(CascadeType.MERGE))
    override var aktor: Aktor,
    @Column(name = "personidenter", nullable = false)
    override var personidenter: String,
    @Column(name = "mottatt", nullable = false)
    val mottatt: LocalDateTime = LocalDateTime.now(),
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: StatusKontoendring = StatusKontoendring.MOTTATT,
    @Column(name = "statustidspunkt", nullable = false)
    var statustidspunkt: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : Personhendelse(personidenter, aktor)

@Entity
class Aktor(
    @Column(nullable = false)
    val aktorid: String,
    @Column
    var publisert: LocalDateTime? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = 0,
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aktor", cascade = arrayOf(CascadeType.MERGE))
    val hendelsemottak: Set<Hendelsemottak> = HashSet()
)

enum class Status {
    MOTTATT,
    KANSELLERT,
    OVERFØRT,
    OVERFØRING_FEILET
}

enum class StatusKontoendring {
    MOTTATT,
    TRUKKET
}

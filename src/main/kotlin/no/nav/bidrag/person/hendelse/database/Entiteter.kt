package no.nav.bidrag.person.hendelse.database


import jakarta.persistence.*
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import java.time.LocalDateTime

@Entity
data class Hendelsemottak(
    val hendelseid: String = "",
    @Enumerated(EnumType.STRING)
    val opplysningstype: Livshendelse.Opplysningstype? = null,
    @Enumerated(EnumType.STRING)
    val endringstype: Livshendelse.Endringstype? = null,
    @Column(name = "personidenter", nullable = true)
    val personidenter: String? = null,
    val tidligereHendelseid: String? = null,
    @Column(length = 5000)
    val hendelse: String = "",
    val master: String = "",
    val offset_pdl: Long = 0L,
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    var status: Status = Status.MOTTATT,
    var statustidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "opprettet", nullable = false, updatable = false)
    val opprettet: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
)

enum class Status {
    MOTTATT,
    KANSELLERT,
    OVERFØRT,
    OVERFØRING_FEILET,
    UNDER_PROSESSERING
}



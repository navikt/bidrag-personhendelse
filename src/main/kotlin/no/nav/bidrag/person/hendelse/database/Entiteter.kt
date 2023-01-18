package no.nav.bidrag.person.hendelse.database


import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
data class Hendelsearkiv(
    val hendelseid: String = "",
    val opplysningstype: String = "",
    val endringstype: String = "",
    val master: String = "",
    val offset_pdl: Long = 0L,

    @Column(name = "personidenter", nullable = true)
    val personidenter: String? = null,
    val tidligereHendelseid: String? = null,
    val hendelse: String = "",

    @Column(name = "opprettet", nullable = false, updatable = false)
    val opprettet: LocalDateTime = LocalDateTime.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

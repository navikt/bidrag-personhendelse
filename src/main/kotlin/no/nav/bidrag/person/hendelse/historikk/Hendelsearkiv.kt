package no.nav.bidrag.person.hendelse.historikk

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "HENDELSEARKIV")
data class Hendelsearkiv(
    val hendelseid: String = "",
    val opplysningstype: String = "",
    val endringstype: String = "",
    val master: String = "",
    val offset: Long = 0L,

    @Column(name = "personidenter", nullable = true)
    val personidenter: String? = null,
    val tidligereHendelseid: String? = null,
    val hendelse: String = "",
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hendelsearkiv_seq")
    @Column(name = "opprettet", nullable = false, updatable = false)
    val opprettet: LocalDateTime = LocalDateTime.now(),
    @SequenceGenerator(name = "hendelsearkiv_seq")
    val id: Long? = null,
)

@Repository
interface HendelsearkivDao : JpaRepository<Hendelsearkiv, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun lagre(hendelsearkiv: Hendelsearkiv): Hendelsearkiv

    fun existsByHendelseidAndOpplysningstype(hendelseid: String, opplysningstype: String): Boolean
}


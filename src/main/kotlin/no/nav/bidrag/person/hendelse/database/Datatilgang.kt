package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

@Repository
interface HendelsearkivDao : JpaRepository<Hendelsearkiv, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(hendelsearkiv: Hendelsearkiv): Hendelsearkiv

    fun existsByHendelseidAndOpplysningstype(hendelseid: String, opplysningstype: String): Boolean
}
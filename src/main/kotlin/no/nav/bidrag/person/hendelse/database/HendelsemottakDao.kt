package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.LockModeType
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.lang.Nullable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
open interface HendelsemottakDao : JpaRepository<Hendelsemottak, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(hendelsemottak: Hendelsemottak): Hendelsemottak

    fun existsByHendelseidAndOpplysningstype(hendelseid: String, opplysningstype: Livshendelse.Opplysningstype): Boolean

    fun findById(id: Int): Hendelsemottak

    @Nullable
    fun findByHendelseidAndStatus(hendelseid: String, status: Status): Hendelsemottak?

    @Query(
        "select ha.id from Hendelsemottak ha " +
                "where ha.status in (no.nav.bidrag.person.hendelse.database.Status.MOTTATT) " +
                "and ha.statustidspunkt < :statustidspunktFør"
    )
    fun henteIdTilHendelserSomSkalSendesVidere(statustidspunktFør: LocalDateTime): Set<Long>
}
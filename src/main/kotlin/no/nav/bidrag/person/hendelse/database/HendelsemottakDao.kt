package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.LockModeType
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.lang.Nullable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface HendelsemottakDao : JpaRepository<Hendelsemottak, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(hendelsemottak: Hendelsemottak): Hendelsemottak

    fun existsByHendelseidAndOpplysningstype(hendelseid: String, opplysningstype: Livshendelse.Opplysningstype): Boolean

    @Nullable
    fun findByHendelseidAndStatus(hendelseid: String, status: Status): Hendelsemottak?

    @Query(
        "select hm.id from Hendelsemottak hm " +
                "where hm.status = no.nav.bidrag.person.hendelse.database.Status.MOTTATT and hm.statustidspunkt < :statustidspunktFør"
    )
    fun idTilHendelserSomErKlarTilOverføring(statustidspunktFør: LocalDateTime): Set<Long>

    @Query(
        "select distinct(hm.aktor.aktorid) from Hendelsemottak hm " +
                "where (hm.aktor.publisert is null or hm.aktor.publisert < :publisertFør) " +
                " and hm.status = no.nav.bidrag.person.hendelse.database.Status.OVERFØRT"
    )
    fun aktøridTilPubliseringsklareOverførteHendelser(publisertFør: LocalDateTime): Set<String>

    @Query("select hm.id from Hendelsemottak hm where hm.status = :status and hm.statustidspunkt < :statustidspunktFør")
    fun henteIdTilHendelser(status: Status, statustidspunktFør: LocalDateTime): Set<Long>

    @Transactional
    fun deleteByIdIn(ider: Set<Long>): Long
}

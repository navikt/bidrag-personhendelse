package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface KontoendringDao : JpaRepository<Kontoendring, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(kontoendring: Kontoendring): Kontoendring

    @Query(
        "select ke.aktor.aktorid from Kontoendring ke " +
            "where ke.status in (no.nav.bidrag.person.hendelse.database.StatusKontoendring.MOTTATT) " +
            "and ke.mottatt < :mottattFør " +
            "and (ke.aktor.publisert is null or ke.aktor.publisert < :publisertFør)"
    )
    fun henteKontoeiereForPublisering(mottattFør: LocalDateTime, publisertFør: LocalDateTime): Set<String>

    fun findByAktorAndStatus(kontoeier: Aktor, status: StatusKontoendring): List<Kontoendring>
}

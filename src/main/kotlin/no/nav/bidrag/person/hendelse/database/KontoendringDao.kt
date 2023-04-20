package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface KontoendringDao : JpaRepository<Kontoendring, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(kontoendring: Kontoendring): Kontoendring

    @Query("select ke.kontoeier from Kontoendring ke where ke.status = :status")
    fun henteKontoeiere(status: StatusKontoendring): Set<String>

    fun findByKontoeierAndStatus(kontoeier: String, status: StatusKontoendring): List<Kontoendring>
}

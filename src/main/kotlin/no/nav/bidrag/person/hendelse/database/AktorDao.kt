package no.nav.bidrag.person.hendelse.database

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Repository
interface AktorDao : JpaRepository<Aktor, Long> {

    fun findByAktorid(aktorid: String): Optional<Aktor>

    @Query("select a.id from Aktor a where a.hendelsemottak is empty and a.publisert < :publisertFør")
    fun henteAktørerSomManglerReferanseTilHendelse(publisertFør: LocalDateTime): Set<Long>

    @Transactional
    fun deleteAktorByIdIn(id: Set<Long>)
}

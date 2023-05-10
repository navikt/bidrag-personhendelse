package no.nav.bidrag.person.hendelse.database

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AktorDao : JpaRepository<Aktor, Long> {

    fun findByAktorid(aktorid: String): Optional<Aktor>
}

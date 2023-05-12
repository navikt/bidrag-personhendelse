package no.nav.bidrag.person.hendelse.database

import io.kotest.matchers.shouldBe
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class AktorTest {

    @Test
    fun `teste equals for Aktør`() {
        // gitt
        val aktør1 = Aktor("1234567891012")
        val aktør2 = Aktor("2345678910123")
        val aktør3 = Aktor("2345678910123", LocalDateTime.now().minusDays(5))

        // hvis
        val aktør1VsAktør2 = aktør1.equals(aktør2)
        val aktør2VsAktør3 = aktør2.equals(aktør3)
        val aktør3VsAktør1 = aktør3.equals(aktør1)
        val aktør2VsNull = aktør2.equals(null)

        // så
        SoftAssertions.assertSoftly {
            aktør1VsAktør2 shouldBe false
            aktør2VsAktør3 shouldBe true
            aktør3VsAktør1 shouldBe false
            aktør2VsNull shouldBe false
        }
    }
}

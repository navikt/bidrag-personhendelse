package no.nav.bidrag.person.hendelse.integrasjon.distribusjon

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.person.hendelse.exception.OverføringFeiletException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.jms.core.JmsTemplate

@ExtendWith(MockKExtension::class)
class MeldingsprodusentTest {

    @MockK
    lateinit var jmsTemplate: JmsTemplate
    lateinit var meldingsprodusent: Meldingsprodusent

    @BeforeEach
    internal fun oppsett() {
        MockKAnnotations.init(this)
        clearAllMocks()
        meldingsprodusent = Meldingsprodusent(jmsTemplate)
    }

    @Test
    fun `skal kaste OverføringFeiletException dersom det oppstår en feil ved sending`() {

        every { jmsTemplate.send(any())} throws Exception("auda!")
        assertThrows<OverføringFeiletException>{
            meldingsprodusent.sendeMeldinger("", listOf(""))
        }
    }
}
package no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.database.Aktor
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CompletableFuture

@ActiveProfiles(Testkonfig.PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
class BidragKafkaMeldingsprodusentTest {

    @MockK
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @MockK
    lateinit var databasetjeneste: Databasetjeneste

    lateinit var bidragKafkaMeldingsprodusent: BidragKafkaMeldingsprodusent

    @BeforeEach
    internal fun oppsett() {
        MockKAnnotations.init(this)
        clearAllMocks()
        bidragKafkaMeldingsprodusent = BidragKafkaMeldingsprodusent(kafkaTemplate, databasetjeneste)
    }

    @Test
    fun `skal produsere meldinger på riktig format`() {
        // gitt
        val aktør = Aktor("123")
        val completeableFuture: CompletableFuture<SendResult<String, String>> = mockk(relaxed = true)
        every { kafkaTemplate.send(any(), any(), any()) } returns completeableFuture

        // hvis
        bidragKafkaMeldingsprodusent.publisereEndringsmelding(aktør, setOf("875", aktør.aktorid))

        // så
        val melding = slot<String>()
        verify(exactly = 1) {
            kafkaTemplate.send(BidragKafkaMeldingsprodusent.BIDRAG_PERSONHENDELSE_TOPIC, aktør.aktorid, capture(melding))
        }

        val streng = "{\"aktørid\":\"123\",\"personidenter\":[\"875\",\"123\"]}"

        Assertions.assertThat(melding.captured).isEqualTo(streng)
    }
}

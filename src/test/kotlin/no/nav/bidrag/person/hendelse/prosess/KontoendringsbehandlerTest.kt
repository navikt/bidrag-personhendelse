package no.nav.bidrag.person.hendelse.prosess

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.person.hendelse.database.Aktor
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Kontoendring
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.BidragPersonklient
import no.nav.bidrag.person.hendelse.testdata.generereAktørid
import no.nav.bidrag.person.hendelse.testdata.generererFødselsnummer
import no.nav.bidrag.person.hendelse.testdata.tilPersonidentDtoer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class KontoendringsbehandlerTest {

    @MockK
    lateinit var mockDatabasetjeneste: Databasetjeneste

    @MockK
    lateinit var mockBidragPersonklient: BidragPersonklient

    lateinit var kontoendringsbehandler: Kontoendringsbehandler

    @BeforeEach
    internal fun oppsett() {
        kontoendringsbehandler = Kontoendringsbehandler(mockBidragPersonklient, mockDatabasetjeneste)
        clearAllMocks()
    }

    @Test
    fun `skal lagre kontoendring`() {
        // gitt
        var kontoeierAktørid = generereAktørid()
        var kontoeierFødselsnummer = generererFødselsnummer()

        var personidentDtoer = tilPersonidentDtoer(setOf(kontoeierAktørid, kontoeierFødselsnummer))

        every { mockBidragPersonklient.henteAlleIdenterForPerson(kontoeierFødselsnummer) } returns personidentDtoer
        every { mockDatabasetjeneste.lagreKontoendring(kontoeierAktørid) } returns Kontoendring(Aktor(kontoeierAktørid))

        // hvis
        kontoendringsbehandler.lagreKontoendring(kontoeierFødselsnummer)

        // så
        val kontoeier = slot<String>()
        verify(exactly = 1) {
            mockDatabasetjeneste.lagreKontoendring(
                capture(kontoeier)
            )
        }
    }
}

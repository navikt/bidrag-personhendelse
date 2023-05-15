package no.nav.bidrag.person.hendelse.skedulering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig
import no.nav.bidrag.person.hendelse.testdata.TeststøtteMeldingsmottak
import no.nav.bidrag.person.hendelse.testdata.generereIdenter
import no.nav.bidrag.person.hendelse.testdata.tilPersonidentDtoer
import no.nav.bidrag.transport.person.Identgruppe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(Testkonfig.PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
class PublisereEndringmeldingerTest {

    @Autowired
    lateinit var teststøtteMeldingsmottak: TeststøtteMeldingsmottak

    @Autowired
    lateinit var databasetjeneste: Databasetjeneste

    @MockK
    lateinit var meldingsprodusent: BidragKafkaMeldingsprodusent

    lateinit var publisereEndringsmeldinger: PublisereEndringsmeldinger

    @BeforeEach
    fun initialisere() {
        MockKAnnotations.init(this)
        clearAllMocks()
        databasetjeneste.hendelsemottakDao.deleteAll()
        publisereEndringsmeldinger = PublisereEndringsmeldinger(
            meldingsprodusent,
            databasetjeneste,
            databasetjeneste.egenskaper
        )
        every { meldingsprodusent.publisereEndringsmelding(any(), any()) }
    }

    @Test
    fun `skal publisere endringsmeldinger for personer med nylig oppdaterte personopplysninger`() {
        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val personidentDtoAktør = personidentDtoer?.find { it.gruppe == Identgruppe.AKTORID }

        teststøtteMeldingsmottak.oppretteOgLagreHendelsemottak(personidentDtoer!!.map { it.ident })
        every { meldingsprodusent.publisereEndringsmelding(any(), any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val aktørid = slot<String>()
        val identer = slot<Set<String>>()
        verify(exactly = 1) {
            meldingsprodusent.publisereEndringsmelding(
                capture(aktørid),
                capture(identer)
            )
        }

        aktørid.asClue { it.captured shouldBe personidentDtoAktør?.ident }
        identer.asClue { it.captured.toString() shouldBe personidenter.toString() }
    }

    @Test
    fun `skal ikke publisere endringsmelding for samme person mer enn én gang innenfor en bestemt periode`() {
        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val personidentDtoAktør = personidentDtoer?.find { it.gruppe == Identgruppe.AKTORID }

        teststøtteMeldingsmottak.oppretteOgLagreHendelsemottak(personidentDtoer!!.map { it.ident })
        every { meldingsprodusent.publisereEndringsmelding(any(), any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val aktørid = slot<String>()
        val identer = slot<Set<String>>()
        verify(exactly = 1) {
            meldingsprodusent.publisereEndringsmelding(
                capture(aktørid),
                capture(identer)
            )
        }

        aktørid.asClue { it.captured shouldBe personidentDtoAktør?.ident }
        identer.asClue { it.captured shouldBe personidenter }
    }
}

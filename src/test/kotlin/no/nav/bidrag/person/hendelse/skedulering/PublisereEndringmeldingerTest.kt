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
import no.nav.bidrag.person.hendelse.database.Aktor
import no.nav.bidrag.person.hendelse.database.Databasetjeneste
import no.nav.bidrag.person.hendelse.database.Hendelsemottak
import no.nav.bidrag.person.hendelse.database.Kontoendring
import no.nav.bidrag.person.hendelse.database.Status
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.BidragPersonklient
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.domene.PersonidentDto
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.domene.Endringsmelding
import no.nav.bidrag.person.hendelse.integrasjon.pdl.domene.Identgruppe
import no.nav.bidrag.person.hendelse.konfigurasjon.Testkonfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.zip.CRC32

@ActiveProfiles(Testkonfig.PROFIL_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Teststarter::class])
class PublisereEndringmeldingerTest {

    @Autowired
    lateinit var databasetjeneste: Databasetjeneste

    @MockK
    lateinit var meldingsprodusent: BidragKafkaMeldingsprodusent

    @MockK
    lateinit var bidragPersonklient: BidragPersonklient

    lateinit var publisereEndringsmeldinger: PublisereEndringsmeldinger

    @BeforeEach
    fun initialisere() {
        MockKAnnotations.init(this)
        clearAllMocks()
        databasetjeneste.hendelsemottakDao.deleteAll()
        databasetjeneste.kontoendringDao.deleteAll()
        databasetjeneste.aktorDao.deleteAll()
        publisereEndringsmeldinger = PublisereEndringsmeldinger(
            meldingsprodusent,
            bidragPersonklient,
            databasetjeneste,
            databasetjeneste.egenskaper
        )
        every { meldingsprodusent.publisereEndringsmelding(any()) }
    }

    @Test
    fun `skal publisere endringsmelding for kontoendring med utløpt venteperiode etter mottak`() {

        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val aktør = personidentDtoer.find { it.gruppe == Identgruppe.AKTORID }

        val mottattTidspunkt = LocalDateTime.now()
            .minusMinutes(databasetjeneste.egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 1)

        val tidspunktSistPublisert = LocalDateTime.now()
            .minusHours(databasetjeneste.egenskaper.generelt.antallTimerSidenForrigePublisering.toLong() - 1)

        oppretteOgLagreKontoendring(aktør!!.ident, mottattTidspunkt, tidspunktSistPublisert)

        every { aktør.let { bidragPersonklient.henteAlleIdenterForPerson(aktør.ident) } } returns personidentDtoer
        every { meldingsprodusent.publisereEndringsmelding(any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val endringsmelding = slot<Endringsmelding>()
        verify(exactly = 1) {
            meldingsprodusent.publisereEndringsmelding(
                capture(endringsmelding)
            )
        }
    }

    @Test
    fun `skal ikke publisere endringsmelding for kontoendring for person med ikke utløpt venteperiode mellom publiseringer`() {
        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val aktør = personidentDtoer.find { it.gruppe == Identgruppe.AKTORID }

        var mottattTidspunktIVenteperiode = LocalDateTime.now()
            .minusMinutes(databasetjeneste.egenskaper.generelt.antallMinutterForsinketVideresending.toLong() - 1)
        val publsertTidspunktEtterVenteperiode = LocalDateTime.now()
            .minusHours(databasetjeneste.egenskaper.generelt.antallTimerSidenForrigePublisering.toLong() + 1)

        oppretteOgLagreKontoendring(
            aktør!!.ident,
            mottattTidspunktIVenteperiode,
            publsertTidspunktEtterVenteperiode
        )

        every { aktør.let { bidragPersonklient.henteAlleIdenterForPerson(aktør.ident) } } returns personidentDtoer
        every { meldingsprodusent.publisereEndringsmelding(any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val endringsmelding = slot<Endringsmelding>()
        verify(exactly = 0) {
            meldingsprodusent.publisereEndringsmelding(
                capture(endringsmelding)
            )
        }
    }

    @Test
    fun `skal publisere endringsmeldinger for personer med nylig oppdaterte personopplysninger`() {
        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val aktør = personidentDtoer.find { it.gruppe == Identgruppe.AKTORID }

        oppretteOgLagreHendelsemottak(personidentDtoer.map { it.ident })
        every { aktør?.let { bidragPersonklient.henteAlleIdenterForPerson(aktør.ident) } } returns personidentDtoer
        every { meldingsprodusent.publisereEndringsmelding(any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val endringsmelding = slot<Endringsmelding>()
        verify(exactly = 1) {
            meldingsprodusent.publisereEndringsmelding(
                capture(endringsmelding)
            )
        }

        endringsmelding.asClue {
            it.captured.aktørid shouldBe aktør?.ident
            it.captured.personidenter.size shouldBe personidentDtoer.size
            it.captured.personidenter shouldBe personidenter
        }
    }

    @Test
    fun `skal ikke publisere endringsmelding for samme person mer enn én gang innenfor en bestemt periode`() {
        // gitt
        val personidenter = generereIdenter()
        val personidentDtoer = tilPersonidentDtoer(personidenter)

        val aktør = personidentDtoer.find { it.gruppe == Identgruppe.AKTORID }

        personidenter.find { it.length == 13 }?.let { oppretteOgLagreKontoendring(it) }
        oppretteOgLagreHendelsemottak(personidentDtoer.map { it.ident })
        every { aktør?.let { bidragPersonklient.henteAlleIdenterForPerson(aktør.ident) } } returns personidentDtoer
        every { meldingsprodusent.publisereEndringsmelding(any()) } returns Unit

        // hvis
        publisereEndringsmeldinger.identifisereOgPublisere()

        // så
        val endringsmelding = slot<Endringsmelding>()
        verify(exactly = 1) {
            meldingsprodusent.publisereEndringsmelding(
                capture(endringsmelding)
            )
        }

        endringsmelding.asClue {
            it.captured.aktørid shouldBe aktør?.ident
            it.captured.personidenter.size shouldBe personidentDtoer.size
            it.captured.personidenter shouldBe personidenter
        }
    }

    fun henteAktør(aktørid: String): Aktor {
        val eksisteredeAktør = databasetjeneste.aktorDao.findByAktorid(aktørid)

        if (eksisteredeAktør.isPresent) {
            return eksisteredeAktør.get()
        } else {
            return databasetjeneste.aktorDao.save(Aktor(aktørid))
        }
    }

    fun oppretteOgLagreKontoendring(
        aktørid: String,
        mottatt: LocalDateTime = LocalDateTime.now()
            .minusHours(databasetjeneste.egenskaper.generelt.antallMinutterForsinketVideresending.toLong() + 1),
        publisert: LocalDateTime? = null
    ): Kontoendring {
        var aktør = henteAktør(aktørid)
        return databasetjeneste.kontoendringDao.save(Kontoendring(aktør, mottatt))
    }

    fun oppretteOgLagreHendelsemottak(personidenter: List<String>, status: Status = Status.OVERFØRT): Hendelsemottak {
        val aktør = henteAktør(personidenter.first { it.length == 13 })

        var mottattHendelse = Hendelsemottak(
            CRC32().value.toString(),
            Livshendelse.Opplysningstype.BOSTEDSADRESSE_V1,
            Livshendelse.Endringstype.OPPRETTET,
            personidenter.toString(),
            aktør
        )

        mottattHendelse.status = status

        return databasetjeneste.hendelsemottakDao.save(mottattHendelse)
    }

    companion object {

        private fun tilPersonidentDtoer(personidenter: Set<String>): Set<PersonidentDto> {
            return personidenter.map {
                var identgruppe = Identgruppe.FOLKEREGISTERIDENT
                if (it.length == 13) {
                    identgruppe = Identgruppe.AKTORID
                }
                PersonidentDto(it, identgruppe, false)
            }.toSet()
        }

        private fun generereIdenter(antall: Int = 2): Set<String> {
            var personidenter = setOf(genereIdent(true), genereIdent(false))
            if (antall > 3) {
                return personidenter
            } else {
                var personidenter = setOf(genereIdent(true), genereIdent(false))
                for (i in 1..antall - 2) {
                    personidenter.plus(genereIdent(false))
                }

                return personidenter
            }
        }

        private fun genereIdent(erAktørid: Boolean): String {
            if (erAktørid) {
                return (10000000000..99999999999).random().toString()
            } else {
                return (1000000000000..9999999999999).random().toString()
            }
        }
    }
}

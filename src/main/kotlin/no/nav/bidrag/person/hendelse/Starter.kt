package no.nav.bidrag.person.hendelse

import no.nav.bidrag.person.hendelse.konfigurasjon.Applikasjonskonfig
import no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper.Wmq
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(Applikasjonskonfig::class)
open class Starter

fun main(args: Array<String>) {
    SpringApplication.run(Starter::class.java, *args)
}

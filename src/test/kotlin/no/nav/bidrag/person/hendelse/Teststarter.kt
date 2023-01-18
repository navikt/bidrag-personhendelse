package no.nav.bidrag.person.hendelse

import no.nav.bidrag.person.hendelse.konfigurasjon.Applikasjonskonfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Import

@Import(Applikasjonskonfig::class)
open class Teststarter

fun main(args: Array<String>) {
    val app = SpringApplicationBuilder(Teststarter::class.java)
        .profiles(args[0])
    app.run(*args)
}

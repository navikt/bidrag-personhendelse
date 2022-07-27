package no.nav.bidrag.person.hendelse

import no.nav.bidrag.person.hendelse.konfigurasjon.ApplicationConfig
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Import

@Import(TokenGeneratorConfiguration::class, ApplicationConfig::class)
class DevLauncher

fun main(args: Array<String>) {
    val app = SpringApplicationBuilder(DevLauncher::class.java)
        .profiles("dev")
    app.run(*args)
}

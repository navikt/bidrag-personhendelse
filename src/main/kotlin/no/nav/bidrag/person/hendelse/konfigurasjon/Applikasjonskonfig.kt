package no.nav.bidrag.person.hendelse.konfigurasjon

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@EnableRetry
@SpringBootConfiguration
@ConfigurationPropertiesScan
@ComponentScan("no.nav.bidrag.person.hendelse")
@EntityScan("no.nav.bidrag.person.hendelse.database")
open class Applikasjonskonfig {
    @Bean
    open fun servletWebServerFactory(): ServletWebServerFactory {
        return JettyServletWebServerFactory()
    }
}

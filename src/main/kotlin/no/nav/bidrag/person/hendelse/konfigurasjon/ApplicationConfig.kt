package no.nav.bidrag.person.hendelse.konfigurasjon

import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootConfiguration
@ComponentScan(
    "no.nav.bidrag.person.hendelse"
)
@ConfigurationPropertiesScan("no.nav.bidrag")
@EnableScheduling
@EnableOAuth2Client(cacheEnabled = true)
@EnableRetry
class ApplicationConfig {
    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory {
        return JettyServletWebServerFactory()
    }
}

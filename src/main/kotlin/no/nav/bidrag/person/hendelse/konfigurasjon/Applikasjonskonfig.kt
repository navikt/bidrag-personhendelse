package no.nav.bidrag.person.hendelse.konfigurasjon

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import no.nav.bidrag.person.hendelse.konfigurasjon.Applikasjonskonfig.Companion.PROFIL_I_SKY
import no.nav.bidrag.person.hendelse.konfigurasjon.Applikasjonskonfig.Companion.PROFIL_LOKAL_POSTGRES
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource

@EnableRetry
@Configuration
@ConfigurationPropertiesScan
@ComponentScan("no.nav.bidrag.person.hendelse")
@EntityScan("no.nav.bidrag.person.hendelse.database")
open class Applikasjonskonfig {
    @Bean
    open fun servletWebServerFactory(): ServletWebServerFactory {
        return JettyServletWebServerFactory()
    }

    companion object {
        const val PROFIL_I_SKY = "i-sky"
        const val PROFIL_LOKAL_POSTGRES = "lokal-postgres"
    }
}

@Configuration
@Profile(PROFIL_I_SKY, PROFIL_LOKAL_POSTGRES)
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
open class SchedulerConfiguration {
    @Bean
    open fun lockProvider(dataSource: DataSource): LockProvider {
        return JdbcTemplateLockProvider(dataSource)
    }
}




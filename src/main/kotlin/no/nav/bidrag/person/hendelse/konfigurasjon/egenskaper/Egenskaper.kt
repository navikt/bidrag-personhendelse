package no.nav.bidrag.person.hendelse.konfigurasjon.egenskaper

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "egenskaper")
data class Egenskaper(val generelt: Generelt, val wmq: Wmq)

@ConfigurationProperties("generelt")
data class Generelt(
    val antallMinutterForsinketVideresending: Int = 120,
    val antallDagerLevetidForUtgaatteHendelser: Int = 7,
    val maksAntallMeldingerSomOverfoeresTilBisysOmGangen: Int = 3400
)

@ConfigurationProperties("egenskaper.wmq")
data class Wmq(
    var host: String,
    val port: Int,
    val channel: String,
    val queueManager: String,
    val username: String,
    val password: String,
    val timeout: Int,
    val applicationName: String,
    val queueNameLivshendelser: String
)

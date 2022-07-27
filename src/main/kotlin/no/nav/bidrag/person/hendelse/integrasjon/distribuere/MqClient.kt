package no.nav.bidrag.person.hendelse.integrasjon.distribuere

import org.springframework.stereotype.Service
import javax.jms.ConnectionFactory
@Service class MqClient(private val connectionFactory: ConnectionFactory)

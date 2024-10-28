package no.nav.bidrag.person.hendelse.exception

import org.apache.kafka.common.KafkaException

class HendelsemottakException(
    feilmelding: String,
) : RuntimeException(feilmelding)

class OverføringFeiletException(
    feilmelding: String,
) : RuntimeException(feilmelding)

class PubliseringFeiletException(
    feilmelding: String,
) : KafkaException(feilmelding)

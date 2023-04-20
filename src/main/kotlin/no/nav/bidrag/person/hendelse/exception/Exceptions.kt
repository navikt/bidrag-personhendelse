package no.nav.bidrag.person.hendelse.exception

class HendelsemottakException(feilmelding: String) : RuntimeException(feilmelding)

class OverføringFeiletException(feilmelding: String) : RuntimeException(feilmelding)

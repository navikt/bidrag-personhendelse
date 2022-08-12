package no.nav.bidrag.person.hendelse.integrasjon.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.person.hendelse.integrasjon.motta.LivshendelseService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Livshendelselytter {

  private final LivshendelseService livshendelseService;

}

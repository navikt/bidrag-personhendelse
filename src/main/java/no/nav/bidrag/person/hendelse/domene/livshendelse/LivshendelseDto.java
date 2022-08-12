package no.nav.bidrag.person.hendelse.domene.livshendelse;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@JsonInclude(Include.NON_NULL)
public class LivshendelseDto {

  private String hendelseId;
  private List<String> personidenter;
  private String master;
  private @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") ZonedDateTime opprettet;
  private String opplysningstype;
  private String endringstype;
  private String tidligereHendelseId;
  private LocalDate doedsdato;
  private LocalDate foedselsdato;
  private LocalDate utflyttingsdato;
  private LocalDate innflyttingsdato;
  private LocalDate sivilstandBekreftelsesdato;
}




package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public record AgendaRequest(@NotBlank @Size(max=120) String nome, @NotNull UUID localId,
                            @NotNull UUID modalidadeId, Long categoriaId, @NotNull LocalDate inicio,
                            @NotNull LocalDate fim, @NotEmpty List<@Valid RegraAgendaRequest> regras) {
    public record RegraAgendaRequest(@NotNull DayOfWeek diaDaSemana, @NotEmpty List<@NotNull LocalTime> horarios) {}
}

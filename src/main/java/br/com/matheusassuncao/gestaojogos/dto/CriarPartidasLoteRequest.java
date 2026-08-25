package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;

public record CriarPartidasLoteRequest(
        @NotNull UUID modalidadeId,
        @NotNull UUID localId,
        Long categoriaId,
        @NotEmpty @Size(max = 200) List<@NotNull @Future OffsetDateTime> inicios,
        @Min(2) Integer capacidade,
        OffsetDateTime inscricoesAbremEm,
        OffsetDateTime inscricoesEncerramEm
) {}

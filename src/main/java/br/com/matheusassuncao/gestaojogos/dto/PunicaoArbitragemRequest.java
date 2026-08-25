package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PunicaoArbitragemRequest(
        @NotBlank String id,
        @NotBlank String time,
        @NotNull UUID jogadorId,
        @NotBlank String tipo,
        @Size(max = 120) String motivo,
        @PositiveOrZero int segundo
) {
}

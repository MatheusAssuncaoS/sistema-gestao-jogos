package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record GolArbitragemRequest(
        @NotBlank String id,
        @NotBlank String time,
        @NotNull UUID jogadorId,
        @PositiveOrZero int segundo
) {
}

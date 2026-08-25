package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DadosArbitragemRequest(
        @Min(0) @Max(30) int acrescimos,
        @NotNull Map<UUID, String> escala,
        @NotNull List<@Valid GolArbitragemRequest> gols,
        @NotNull List<@Valid PunicaoArbitragemRequest> punicoes
) {
    public static DadosArbitragemRequest vazio() {
        return new DadosArbitragemRequest(0, Map.of(), List.of(), List.of());
    }
}

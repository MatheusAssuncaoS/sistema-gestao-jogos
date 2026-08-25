package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.StatusArbitragem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AtualizarEstadoArbitragemRequest(
        @NotNull StatusArbitragem status,
        @PositiveOrZero int segundos,
        @NotNull @Valid DadosArbitragemRequest dados,
        @PositiveOrZero int versao
) {
}

package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CriarPartidaRequest(

        @NotNull(message = "A modalidade é obrigatória.")
        UUID modalidadeId,

        @NotNull(message = "O local é obrigatório.")
        UUID localId,

        /**
         * Opcional e meramente informativa: indica o nível esperado da partida,
         * sem restringir quem pode se inscrever. O equilíbrio entre os times
         * vem do balanceamento manual (RN10).
         */
        Long categoriaId,

        @NotNull(message = "A data e o horário são obrigatórios.")
        @Future(message = "A partida deve ser agendada para uma data futura.")
        OffsetDateTime inicio,

        @Min(value = 2, message = "A capacidade mínima é de 2 jogadores.")
        Integer capacidade,

        OffsetDateTime inscricoesAbremEm,

        OffsetDateTime inscricoesEncerramEm
) {

    /** RN02: 16 jogadores de linha no futebol. */
    public Integer capacidadeOuPadrao() {
        return capacidade != null ? capacidade : 16;
    }
}

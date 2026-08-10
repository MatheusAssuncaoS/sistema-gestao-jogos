package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EditarPartidaRequest(

        @NotNull(message = "O local é obrigatório.")
        UUID localId,

        Long categoriaId,

        @NotNull(message = "A data e o horário são obrigatórios.")
        @Future(message = "A partida deve ser agendada para uma data futura.")
        OffsetDateTime inicio,

        OffsetDateTime inscricoesAbremEm,

        OffsetDateTime inscricoesEncerramEm,

        /**
         * Versão que o cliente carregou. O lock otimista compara com a versão
         * atual no banco: se outra pessoa editou nesse meio-tempo, a operação
         * é rejeitada em vez de sobrescrever silenciosamente.
         */
        @NotNull(message = "A versão da partida é obrigatória.")
        Integer versao
) {
}

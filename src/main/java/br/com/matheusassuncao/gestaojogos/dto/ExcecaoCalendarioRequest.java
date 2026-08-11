package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.TipoExcecao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExcecaoCalendarioRequest(

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres.")
        String descricao,

        @NotNull(message = "O tipo é obrigatório.")
        TipoExcecao tipo,

        @NotNull(message = "A data inicial é obrigatória.")
        LocalDate inicio,

        @NotNull(message = "A data final é obrigatória.")
        LocalDate fim
) {
}

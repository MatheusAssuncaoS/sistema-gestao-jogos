package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.SituacaoAssociativa;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AprovarJogadorRequest(

        @Size(max = 50, message = "A matrícula deve ter no máximo 50 caracteres.")
        String matriculaAssociado,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

        @NotNull(message = "A situação associativa é obrigatória.")
        SituacaoAssociativa situacaoAssociativa
) {
}

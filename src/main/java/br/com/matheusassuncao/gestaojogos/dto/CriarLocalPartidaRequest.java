package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarLocalPartidaRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.") String nome,
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.") String descricao
) {}

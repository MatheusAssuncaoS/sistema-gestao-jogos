package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarCategoriaRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres.") String nome,
        @NotNull(message = "O peso é obrigatório.")
        @Min(value = 1, message = "Selecione um nível de habilidade válido.")
        @Max(value = 3, message = "O nível de habilidade deve ser Iniciante, Intermediário ou Avançado.") Integer peso
) {}

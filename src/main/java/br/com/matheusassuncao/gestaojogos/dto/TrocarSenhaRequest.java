package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(

        @NotBlank(message = "A senha atual é obrigatória.")
        String senhaAtual,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String novaSenha
) {
}

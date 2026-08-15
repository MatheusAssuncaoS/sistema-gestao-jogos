package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record RedefinirSenhaRequest(

        @NotBlank(message = "O token é obrigatório.")
        String token,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).+$", message = "A senha deve ter letra maiúscula, minúscula, número e caractere especial.")
        String novaSenha
) {
}

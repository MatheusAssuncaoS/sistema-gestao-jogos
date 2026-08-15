package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAtualizarUsuarioRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 200) String email,
        @NotNull Long versao
) {
}

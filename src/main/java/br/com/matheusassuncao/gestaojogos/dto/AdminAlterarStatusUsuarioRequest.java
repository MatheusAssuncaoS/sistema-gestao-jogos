package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import jakarta.validation.constraints.NotNull;

public record AdminAlterarStatusUsuarioRequest(
        @NotNull StatusUsuario status,
        @NotNull Long versao
) {
}

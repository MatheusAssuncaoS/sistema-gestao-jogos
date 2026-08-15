package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.LocalPartida;

import java.util.UUID;

public record LocalPartidaResponse(
        UUID id,
        String nome,
        String descricao
) {

    public static LocalPartidaResponse de(LocalPartida local) {
        return new LocalPartidaResponse(local.getId(), local.getNome(), local.getDescricao());
    }
}

package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.LocalPartida;

import java.util.UUID;

public record LocalPartidaResponse(
        UUID id,
        String nome,
        String descricao,
        Boolean ativo,
        java.util.Set<UUID> modalidadeIds
) {

    public static LocalPartidaResponse de(LocalPartida local) {
        return new LocalPartidaResponse(local.getId(), local.getNome(), local.getDescricao(), local.getAtivo(), local.getModalidades().stream().map(m -> m.getId()).collect(java.util.stream.Collectors.toSet()));
    }
}

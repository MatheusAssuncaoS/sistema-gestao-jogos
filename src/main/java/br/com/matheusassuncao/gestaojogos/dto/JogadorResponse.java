package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.Jogador;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JogadorResponse(
        UUID id,
        UUID usuarioId,
        String nome,
        String email,
        String matriculaAssociado,
        String categoria,
        String situacaoAssociativa,
        OffsetDateTime aprovadoEm
) {

    public static JogadorResponse de(Jogador jogador) {
        return new JogadorResponse(
                jogador.getId(),
                jogador.getUsuario().getId(),
                jogador.getUsuario().getNome(),
                jogador.getUsuario().getEmail(),
                jogador.getMatriculaAssociado(),
                jogador.getCategoria() != null ? jogador.getCategoria().getNome() : null,
                jogador.getSituacaoAssociativa().name(),
                jogador.getAprovadoEm()
        );
    }
}

package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.Inscricao;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Visão do organizador sobre quem está inscrito (UC17).
 */
public record InscritoResponse(
        UUID inscricaoId,
        UUID jogadorId,
        String nome,
        String categoria,
        String status,
        OffsetDateTime dataSolicitacao
) {

    public static InscritoResponse de(Inscricao inscricao) {
        return new InscritoResponse(
                inscricao.getId(),
                inscricao.getJogador().getId(),
                inscricao.getJogador().getUsuario().getNome(),
                inscricao.getJogador().getCategoria() != null
                        ? inscricao.getJogador().getCategoria().getNome()
                        : null,
                inscricao.getStatus().name(),
                inscricao.getDataSolicitacao()
        );
    }
}

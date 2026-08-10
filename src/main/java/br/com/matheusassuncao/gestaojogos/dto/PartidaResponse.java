package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.Equipe;
import br.com.matheusassuncao.gestaojogos.dominio.Partida;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PartidaResponse(
        UUID id,
        String modalidade,
        String local,
        String categoria,
        OffsetDateTime inicio,
        Integer capacidade,
        String status,
        OffsetDateTime inscricoesAbremEm,
        OffsetDateTime inscricoesEncerramEm,
        Boolean escalaPublicada,
        Integer versao,
        List<EquipeResponse> equipes
) {

    public record EquipeResponse(UUID id, String nome, String cor, Integer capacidade) {

        static EquipeResponse de(Equipe equipe) {
            return new EquipeResponse(
                    equipe.getId(),
                    equipe.getNome(),
                    equipe.getCor(),
                    equipe.getCapacidade()
            );
        }
    }

    public static PartidaResponse de(Partida partida) {
        return new PartidaResponse(
                partida.getId(),
                partida.getModalidade().getNome(),
                partida.getLocal().getNome(),
                partida.getCategoria() != null ? partida.getCategoria().getNome() : null,
                partida.getInicio(),
                partida.getCapacidade(),
                partida.getStatus().name(),
                partida.getInscricoesAbremEm(),
                partida.getInscricoesEncerramEm(),
                partida.getEscalaPublicada(),
                partida.getVersao(),
                partida.getEquipes().stream()
                        .map(EquipeResponse::de)
                        .toList()
        );
    }
}

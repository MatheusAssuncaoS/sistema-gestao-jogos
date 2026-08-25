package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.StatusArbitragem;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EstadoArbitragemResponse(
        UUID partidaId,
        StatusArbitragem status,
        int segundos,
        DadosArbitragemRequest dados,
        int versao,
        OffsetDateTime atualizadoEm
) {
}

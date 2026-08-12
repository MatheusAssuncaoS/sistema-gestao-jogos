package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.Inscricao;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InscricaoResponse(
        UUID id,
        UUID partidaId,
        OffsetDateTime inicioDaPartida,
        String local,
        String status,
        OffsetDateTime dataSolicitacao,
        OffsetDateTime dataConfirmacao,
        String equipe
) {

    public static InscricaoResponse de(Inscricao inscricao) {
        return new InscricaoResponse(
                inscricao.getId(),
                inscricao.getPartida().getId(),
                inscricao.getPartida().getInicio(),
                inscricao.getPartida().getLocal().getNome(),
                inscricao.getStatus().name(),
                inscricao.getDataSolicitacao(),
                inscricao.getDataConfirmacao(),
                // RN05: a cor do colete só é revelada após a publicação da
                // escalação, que entra no Marco 2.
                null
        );
    }
}

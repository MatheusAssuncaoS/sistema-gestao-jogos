package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.Modalidade;

import java.util.UUID;

public record ModalidadeResponse(
        UUID id,
        String nome
) {

    public static ModalidadeResponse de(Modalidade modalidade) {
        return new ModalidadeResponse(modalidade.getId(), modalidade.getNome());
    }
}

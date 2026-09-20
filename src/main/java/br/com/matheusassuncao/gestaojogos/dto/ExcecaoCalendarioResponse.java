package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.ExcecaoCalendario;
import br.com.matheusassuncao.gestaojogos.dominio.TipoExcecao;

import java.time.LocalDate;
import java.util.UUID;

public record ExcecaoCalendarioResponse(
        UUID id,
        String descricao,
        TipoExcecao tipo,
        LocalDate inicio,
        LocalDate fim,
        Boolean ativo,
        UUID localId,
        String local
) {

    public static ExcecaoCalendarioResponse de(ExcecaoCalendario excecao) {
        return new ExcecaoCalendarioResponse(
                excecao.getId(),
                excecao.getDescricao(),
                excecao.getTipo(),
                excecao.getInicio(),
                excecao.getFim(),
                excecao.getAtivo(),
                excecao.getLocal() == null ? null : excecao.getLocal().getId(),
                excecao.getLocal() == null ? null : excecao.getLocal().getNome()
        );
    }
}

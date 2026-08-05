package br.com.matheusassuncao.gestaojogos.dominio;

/**
 * Ciclo de vida de uma partida.
 *
 * LOTADA é informativo: a partida continua recebendo inscrições na lista de
 * espera (RN12) e volta para ABERTA quando um cancelamento libera vaga (RN15).
 */
public enum StatusPartida {

    RASCUNHO,
    ABERTA,
    LOTADA,
    ENCERRADA,
    FINALIZADA,
    CANCELADA;

    public boolean aceitaEdicao() {
        return this == RASCUNHO || this == ABERTA;
    }

    public boolean estaEncerrada() {
        return this == ENCERRADA || this == FINALIZADA || this == CANCELADA;
    }
}

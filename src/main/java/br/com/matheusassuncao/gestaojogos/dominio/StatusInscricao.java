package br.com.matheusassuncao.gestaojogos.dominio;

/**
 * Estados da inscrição.
 *
 * Não existe estado PROMOVIDA: a promoção da lista de espera (RN12, Marco 2)
 * é registrada pela mudança para CONFIRMADA e pelo campo dataPromocao.
 *
 * PRESENTE e AUSENTE consomem o estado CONFIRMADA; a dataConfirmacao preserva
 * a informação de quem estava confirmado antes da chamada.
 */
public enum StatusInscricao {

    CONFIRMADA,
    LISTA_ESPERA,
    CANCELADA,
    PRESENTE,
    AUSENTE;

    public boolean estaAtiva() {
        return this != CANCELADA;
    }

    public boolean ocupaVaga() {
        return this == CONFIRMADA || this == PRESENTE || this == AUSENTE;
    }
}

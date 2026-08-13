package br.com.matheusassuncao.gestaojogos.dominio;

/**
 * RECUSADO e BLOQUEADO não são sinônimos: RECUSADO significa que a pessoa
 * não é sócia cadastrada na instituição (decisão reversível via
 * reabrirCadastro(), volta para PENDENTE). BLOQUEADO é reservado para
 * bloqueio por débito financeiro, uma situação independente de ser sócio.
 */
public enum StatusUsuario {
    PENDENTE, ATIVO, BLOQUEADO, INATIVO, RECUSADO
}
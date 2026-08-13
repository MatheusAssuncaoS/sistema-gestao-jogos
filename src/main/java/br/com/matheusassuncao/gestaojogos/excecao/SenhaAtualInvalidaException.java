package br.com.matheusassuncao.gestaojogos.excecao;

/**
 * A senha atual informada numa troca de senha não confere. Traduzida em
 * HTTP 400 pelo TratadorDeErros — 401 é reservado para quem não está
 * autenticado, e aqui o usuário está autenticado, só errou um campo.
 */
public class SenhaAtualInvalidaException extends RuntimeException {
    public SenhaAtualInvalidaException(String mensagem) {
        super(mensagem);
    }
}

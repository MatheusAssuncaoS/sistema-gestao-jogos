package br.com.matheusassuncao.gestaojogos.excecao;

/**
 * A senha atual informada numa troca de senha não confere. Traduzida em
 * HTTP 401 pelo TratadorDeErros.
 */
public class SenhaAtualInvalidaException extends RuntimeException {
    public SenhaAtualInvalidaException(String mensagem) {
        super(mensagem);
    }
}

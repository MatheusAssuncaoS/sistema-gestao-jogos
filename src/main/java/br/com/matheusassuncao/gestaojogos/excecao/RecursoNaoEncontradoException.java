package br.com.matheusassuncao.gestaojogos.excecao;

/**
 * Recurso solicitado não existe. Traduzida em HTTP 404 pelo TratadorDeErros.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}

package br.com.matheusassuncao.gestaojogos.excecao;

/**
 * Violação de uma regra de negócio do sistema. Traduzida em HTTP 409
 * pelo TratadorDeErros.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}

package br.com.matheusassuncao.gestaojogos.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centraliza a tradução de exceções em respostas HTTP, usando o formato
 * ProblemDetail (RFC 7807).
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacao(MethodArgumentNotValidException excecao) {
        Map<String, String> erros = new HashMap<>();
        excecao.getBindingResult().getFieldErrors().forEach(erro ->
                erros.put(erro.getField(), erro.getDefaultMessage())
        );

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Há campos inválidos na requisição."
        );
        problema.setTitle("Dados inválidos");
        problema.setProperty("campos", erros);

        return problema;
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail tratarRegraNegocio(RegraNegocioException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                excecao.getMessage()
        );
        problema.setTitle("Regra de negócio violada");

        return problema;
    }

    /**
     * Credenciais inválidas e conta inexistente devolvem a mesma resposta,
     * para não revelar quais e-mails estão cadastrados.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail tratarCredenciaisInvalidas(BadCredentialsException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "E-mail ou senha inválidos."
        );
        problema.setTitle("Falha na autenticação");

        return problema;
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail tratarContaInativa(DisabledException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Esta conta está inativa."
        );
        problema.setTitle("Acesso negado");

        return problema;
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail tratarContaBloqueada(LockedException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Esta conta está bloqueada."
        );
        problema.setTitle("Acesso negado");

        return problema;
    }
}

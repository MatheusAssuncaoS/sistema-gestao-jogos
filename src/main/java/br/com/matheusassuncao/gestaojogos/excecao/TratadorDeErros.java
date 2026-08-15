package br.com.matheusassuncao.gestaojogos.excecao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import org.springframework.security.access.AccessDeniedException;

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

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarRecursoNaoEncontrado(RecursoNaoEncontradoException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                excecao.getMessage()
        );
        problema.setTitle("Recurso não encontrado");

        return problema;
    }

    /**
     * 400, não 401: o usuário está autenticado, só informou a senha atual
     * errada. Um 401 aqui arriscaria ser tratado como sessão expirada por
     * quem consome a API. Devolve em "campos" pelo mesmo motivo que a
     * validação de formulário devolve: para o front apontar o erro no
     * campo certo.
     */
    @ExceptionHandler(SenhaAtualInvalidaException.class)
    public ProblemDetail tratarSenhaAtualInvalida(SenhaAtualInvalidaException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                excecao.getMessage()
        );
        problema.setTitle("Senha atual inválida");
        problema.setProperty("campos", Map.of("senhaAtual", excecao.getMessage()));

        return problema;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail tratarAcessoNegado(AccessDeniedException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Você não tem permissão para executar esta ação."
        );
        problema.setTitle("Acesso negado");

        return problema;
    }

    /**
     * Lock otimista: duas edições simultâneas da mesma partida. A segunda é
     * rejeitada para não sobrescrever a primeira sem que ninguém perceba.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail tratarConflitoDeEdicao(OptimisticLockingFailureException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Este registro foi alterado por outra pessoa. Recarregue e tente novamente."
        );
        problema.setTitle("Conflito de edição");

        return problema;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail tratarRegistroEmUso(DataIntegrityViolationException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Este usuário possui histórico ou vínculos com outros registros e não pode ser excluído."
        );
        problema.setTitle("Registro em uso");
        return problema;
    }
}

package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dto.RedefinirSenhaRequest;
import br.com.matheusassuncao.gestaojogos.dto.SolicitarRecuperacaoRequest;
import br.com.matheusassuncao.gestaojogos.servico.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC03: recuperar senha.
 */
@RestController
@RequestMapping("/api/auth/recuperacao-senha")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public RecuperacaoSenhaController(RecuperacaoSenhaService recuperacaoSenhaService) {
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    /**
     * Responde 204 mesmo quando o e-mail não existe, para não revelar
     * quais contas estão cadastradas.
     */
    @PostMapping("/solicitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void solicitar(@RequestBody @Valid SolicitarRecuperacaoRequest request) {
        recuperacaoSenhaService.solicitar(request.email());
    }

    @PostMapping("/redefinir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinir(@RequestBody @Valid RedefinirSenhaRequest request) {
        recuperacaoSenhaService.redefinir(request.token(), request.novaSenha());
    }
}

package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.CadastroRequest;
import br.com.matheusassuncao.gestaojogos.dto.LoginRequest;
import br.com.matheusassuncao.gestaojogos.dto.TrocarSenhaRequest;
import br.com.matheusassuncao.gestaojogos.dto.UsuarioResponse;
import br.com.matheusassuncao.gestaojogos.servico.AuthService;
import br.com.matheusassuncao.gestaojogos.servico.TrocaSenhaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TrocaSenhaService trocaSenhaService;

    public AuthController(AuthService authService, TrocaSenhaService trocaSenhaService) {
        this.authService = authService;
        this.trocaSenhaService = trocaSenhaService;
    }

    /**
     * UC01: cadastrar-se.
     */
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid CadastroRequest request) {
        Usuario usuario = authService.cadastrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UsuarioResponse.de(usuario));
    }

    /**
     * UC02: realizar login. A sessão é devolvida no cookie JSESSIONID.
     */
    @PostMapping("/login")
    public UsuarioResponse login(@RequestBody @Valid LoginRequest request,
                                 HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse) {

        Usuario usuario = authService.autenticar(
                request.email(),
                request.senha(),
                servletRequest,
                servletResponse
        );

        return UsuarioResponse.de(usuario);
    }

    /**
     * Retorna o usuário da sessão atual. Útil para o cliente descobrir
     * quem está logado e quais papéis possui.
     */
    @GetMapping("/eu")
    public UsuarioResponse eu(@AuthenticationPrincipal UserDetails autenticado) {
        return UsuarioResponse.de(authService.buscarPorEmail(autenticado.getUsername()));
    }

    /**
     * Troca de senha pelo próprio usuário autenticado, exigindo a senha
     * atual. É o destino da tela de troca obrigatória quando a senha é
     * provisória.
     */
    @PostMapping("/trocar-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@RequestBody @Valid TrocarSenhaRequest request,
                            @AuthenticationPrincipal UserDetails autenticado) {
        trocaSenhaService.trocar(autenticado.getUsername(), request.senhaAtual(), request.novaSenha());
    }
}

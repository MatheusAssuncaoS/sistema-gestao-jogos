package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.AtualizarDadosRequest;
import br.com.matheusassuncao.gestaojogos.dto.UsuarioResponse;
import br.com.matheusassuncao.gestaojogos.servico.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * UC04: atualizar dados pessoais do usuário autenticado.
     *
     * Se o e-mail for alterado, a sessão atual continua válida até o logout,
     * mas o próximo login usará o novo e-mail.
     */
    @PutMapping("/eu")
    public UsuarioResponse atualizarDados(@AuthenticationPrincipal UserDetails autenticado,
                                          @RequestBody @Valid AtualizarDadosRequest request) {

        Usuario usuario = usuarioService.atualizarDados(autenticado.getUsername(), request);

        return UsuarioResponse.de(usuario);
    }
}

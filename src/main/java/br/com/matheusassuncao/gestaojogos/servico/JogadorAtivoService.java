package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.JogadorRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Listagem de jogadores ativos e redefinição de senha pelo administrador.
 * Fica fora de AprovacaoJogadorService porque não é aprovação de cadastro.
 */
@Service
public class JogadorAtivoService {

    private final UsuarioRepository usuarioRepository;
    private final JogadorRepository jogadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    public JogadorAtivoService(UsuarioRepository usuarioRepository,
                               JogadorRepository jogadorRepository,
                               PasswordEncoder passwordEncoder,
                               SessionRegistry sessionRegistry) {
        this.usuarioRepository = usuarioRepository;
        this.jogadorRepository = jogadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Jogadores ativos, com busca opcional por nome ou e-mail.
     */
    @Transactional(readOnly = true)
    public List<Jogador> listarAtivos(String busca) {
        String termo = busca == null ? "" : busca.trim();
        return jogadorRepository.buscarPorStatusUsuarioEBusca(StatusUsuario.ATIVO, termo);
    }

    /**
     * Redefine a senha de um jogador ativo e encerra as sessões que ele
     * tiver abertas: sem isso, quem já estivesse logado continuaria
     * autenticado com a senha antiga já trocada.
     */
    @Transactional
    public void redefinirSenha(UUID usuarioId, String novaSenha, boolean exigirTrocaNoProximoLogin) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new RegraNegocioException("Só é possível redefinir a senha de jogadores ativos.");
        }

        usuario.redefinirSenha(passwordEncoder.encode(novaSenha), exigirTrocaNoProximoLogin);
        encerrarSessoesAtivas(usuario.getEmail());
    }

    private void encerrarSessoesAtivas(String email) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof UserDetails detalhes
                        && detalhes.getUsername().equalsIgnoreCase(email))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(sessao -> sessao.expireNow());
    }
}

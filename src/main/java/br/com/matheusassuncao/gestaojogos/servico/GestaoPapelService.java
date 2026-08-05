package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.Papel;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * UC19: gerenciar organizadores.
 *
 * O papel de organizador é independente do de jogador: um associado pode
 * acumular os dois, e um organizador não precisa ser jogador.
 */
@Service
public class GestaoPapelService {

    private static final Logger log = LoggerFactory.getLogger(GestaoPapelService.class);

    private static final String PAPEL_ORGANIZADOR = "ORGANIZADOR";

    private final UsuarioRepository usuarioRepository;
    private final PapelRepository papelRepository;

    public GestaoPapelService(UsuarioRepository usuarioRepository,
                              PapelRepository papelRepository) {
        this.usuarioRepository = usuarioRepository;
        this.papelRepository = papelRepository;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAllByOrderByCriadoEm();
    }

    /**
     * Idempotente: conceder um papel que o usuário já possui não altera nada
     * nem devolve erro.
     */
    @Transactional
    public Usuario concederOrganizador(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);

        if (usuario.getStatus() == StatusUsuario.BLOQUEADO
                || usuario.getStatus() == StatusUsuario.INATIVO) {
            throw new RegraNegocioException(
                    "Não é possível conceder papéis a um usuário bloqueado ou inativo."
            );
        }

        Papel organizador = buscarPapelOrganizador();

        if (usuario.possuiPapel(PAPEL_ORGANIZADOR)) {
            return usuario;
        }

        usuario.adicionarPapel(organizador);
        usuario.ativar();

        log.info("Papel de organizador concedido ao usuário {}.", usuarioId);

        return usuario;
    }

    /**
     * Idempotente: revogar de quem não tem o papel não devolve erro.
     *
     * Como a autenticação é por sessão, os papéis são carregados no login:
     * a revogação só tem efeito a partir da próxima autenticação.
     */
    @Transactional
    public Usuario revogarOrganizador(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);

        if (!usuario.possuiPapel(PAPEL_ORGANIZADOR)) {
            return usuario;
        }

        usuario.removerPapel(buscarPapelOrganizador());

        log.info("Papel de organizador revogado do usuário {}.", usuarioId);

        return usuario;
    }

    private Usuario buscar(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private Papel buscarPapelOrganizador() {
        return papelRepository.findByNome(PAPEL_ORGANIZADOR)
                .orElseThrow(() -> new IllegalStateException(
                        "Papel ORGANIZADOR não encontrado. Verifique a migration V1."
                ));
    }
}

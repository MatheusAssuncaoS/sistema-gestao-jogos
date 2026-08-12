package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.Categoria;
import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.Papel;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.AprovarJogadorRequest;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.CategoriaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.JogadorRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * UC27: aprovar cadastro de jogador.
 */
@Service
public class AprovacaoJogadorService {

    private final UsuarioRepository usuarioRepository;
    private final JogadorRepository jogadorRepository;
    private final CategoriaRepository categoriaRepository;
    private final PapelRepository papelRepository;

    public AprovacaoJogadorService(UsuarioRepository usuarioRepository,
                                   JogadorRepository jogadorRepository,
                                   CategoriaRepository categoriaRepository,
                                   PapelRepository papelRepository) {
        this.usuarioRepository = usuarioRepository;
        this.jogadorRepository = jogadorRepository;
        this.categoriaRepository = categoriaRepository;
        this.papelRepository = papelRepository;
    }

    /**
     * Usuários cadastrados que ainda não têm perfil de jogador.
     */
    @Transactional(readOnly = true)
    public List<Usuario> listarPendentes() {
        return usuarioRepository.findByStatusAndSemPerfilDeJogador(StatusUsuario.PENDENTE);
    }

    /**
     * Categorias disponíveis para o formulário de aprovação.
     */
    @Transactional(readOnly = true)
    public List<Categoria> listarCategoriasAtivas() {
        return categoriaRepository.findByAtivoTrue();
    }

    /**
     * Cadastros recusados, candidatos a voltar para a fila de aprovação.
     */
    @Transactional(readOnly = true)
    public List<Usuario> listarRecusados() {
        return usuarioRepository.findByStatusAndSemPerfilDeJogador(StatusUsuario.RECUSADO);
    }

    /**
     * Cria o perfil de jogador, atribui o papel JOGADOR e ativa a conta.
     * A partir daqui o associado consegue efetivamente usar o sistema.
     */
    @Transactional
    public Jogador aprovar(UUID usuarioId, AprovarJogadorRequest request, String emailDoAdministrador) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (jogadorRepository.existsByUsuarioId(usuarioId)) {
            throw new RegraNegocioException("Este cadastro já foi aprovado.");
        }

        if (usuario.getStatus() == StatusUsuario.BLOQUEADO
                || usuario.getStatus() == StatusUsuario.INATIVO) {
            throw new RegraNegocioException(
                    "Não é possível aprovar um cadastro bloqueado ou inativo."
            );
        }

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));

        Usuario administrador = usuarioRepository.findByEmailIgnoreCase(emailDoAdministrador)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador não encontrado."));

        Papel papelJogador = papelRepository.findByNome("JOGADOR")
                .orElseThrow(() -> new IllegalStateException(
                        "Papel JOGADOR não encontrado. Verifique a migration V1."
                ));

        usuario.ativar();
        usuario.adicionarPapel(papelJogador);

        Jogador jogador = new Jogador(
                usuario,
                normalizar(request.matriculaAssociado()),
                categoria,
                request.situacaoAssociativa(),
                administrador
        );

        return jogadorRepository.save(jogador);
    }

    /**
     * Recusa um cadastro pendente. Diferente da aprovação, não cria perfil
     * de jogador nem exige categoria: só marca o usuário como RECUSADO.
     */
    @Transactional
    public Usuario recusar(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (usuario.getStatus() != StatusUsuario.PENDENTE) {
            throw new RegraNegocioException("Apenas cadastros pendentes podem ser recusados.");
        }

        usuario.recusar();
        return usuarioRepository.save(usuario);
    }

    /**
     * Reabre um cadastro recusado, devolvendo-o para a fila de aprovação.
     * A decisão de recusa pode ter sido um engano ou ficar desatualizada
     * quando a pessoa se associa depois, então precisa ser reversível.
     */
    @Transactional
    public void reabrir(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (usuario.getStatus() != StatusUsuario.RECUSADO) {
            throw new RegraNegocioException(
                    "Apenas cadastros recusados podem voltar para a fila de aprovação."
            );
        }

        usuario.reabrirCadastro();
    }

    private String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
}

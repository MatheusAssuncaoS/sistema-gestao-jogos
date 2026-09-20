package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.Categoria;
import br.com.matheusassuncao.gestaojogos.dominio.LocalPartida;
import br.com.matheusassuncao.gestaojogos.dominio.Modalidade;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.repositorio.CategoriaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.LocalPartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.ModalidadeRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.JogadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import br.com.matheusassuncao.gestaojogos.dto.LocalPartidaResponse;
import br.com.matheusassuncao.gestaojogos.dto.ModalidadeResponse;

@Service
public class AdminConfiguracaoService {
    private final ModalidadeRepository modalidades;
    private final LocalPartidaRepository locais;
    private final CategoriaRepository categorias;
    private final PartidaRepository partidas;
    private final JogadorRepository jogadores;

    public AdminConfiguracaoService(ModalidadeRepository modalidades, LocalPartidaRepository locais,
                                    CategoriaRepository categorias,
                                    PartidaRepository partidas,
                                    JogadorRepository jogadores) {
        this.modalidades = modalidades;
        this.locais = locais;
        this.categorias = categorias;
        this.partidas = partidas;
        this.jogadores = jogadores;
    }

    @Transactional(readOnly = true)
    public List<LocalPartidaResponse> listarLocais() { return locais.findAll().stream().map(LocalPartidaResponse::de).toList(); }
    @Transactional(readOnly = true)
    public List<ModalidadeResponse> listarModalidades() { return modalidades.findAll().stream().map(ModalidadeResponse::de).toList(); }

    private Set<Modalidade> buscarModalidades(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) throw new RegraNegocioException("Selecione pelo menos uma modalidade.");
        var encontradas = modalidades.findAllById(ids);
        if (encontradas.size() != ids.size()) throw new RegraNegocioException("Uma das modalidades não existe.");
        return new HashSet<>(encontradas);
    }

    @Transactional
    public Modalidade criarModalidade(String nome, Boolean ativo) {
        modalidades.findByNomeIgnoreCase(nome.trim()).ifPresent(item -> { throw new RegraNegocioException("Já existe uma modalidade com esse nome."); });
        var modalidade = new Modalidade(nome);
        modalidade.definirAtivo(ativo == null || ativo);
        return modalidades.save(modalidade);
    }

    @Transactional
    public Modalidade editarModalidade(UUID modalidadeId, String nome, Boolean ativo) {
        Modalidade modalidade = modalidades.findById(modalidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modalidade não encontrada."));
        modalidades.findByNomeIgnoreCase(nome.trim())
                .filter(existente -> !existente.getId().equals(modalidadeId))
                .ifPresent(item -> { throw new RegraNegocioException("Já existe uma modalidade com esse nome."); });
        modalidade.atualizarNome(nome);
        if (ativo != null) modalidade.definirAtivo(ativo);
        return modalidade;
    }

    @Transactional
    public void excluirModalidade(UUID modalidadeId) {
        Modalidade modalidade = modalidades.findById(modalidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modalidade não encontrada."));
        if (partidas.existsByModalidade_Id(modalidadeId)) {
            throw new RegraNegocioException("A modalidade não pode ser excluída porque está vinculada a uma ou mais partidas.");
        }
        if (locais.existsByModalidades_Id(modalidadeId)) throw new RegraNegocioException("Desvincule os locais antes de excluir a modalidade.");
        modalidades.delete(modalidade);
    }

    @Transactional
    public LocalPartida criarLocal(String nome, String descricao, Set<UUID> modalidadeIds, Boolean ativo) {
        locais.findByNomeIgnoreCase(nome.trim()).ifPresent(item -> { throw new RegraNegocioException("Já existe um local com esse nome."); });
        var local = new LocalPartida(nome, descricao);
        local.definirModalidades(buscarModalidades(modalidadeIds));
        local.definirAtivo(ativo == null || ativo);
        return locais.save(local);
    }

    @Transactional
    public LocalPartida editarLocal(UUID localId, String nome, String descricao, Set<UUID> modalidadeIds, Boolean ativo) {
        LocalPartida local = locais.findById(localId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
        locais.findByNomeIgnoreCase(nome.trim())
                .filter(existente -> !existente.getId().equals(localId))
                .ifPresent(item -> { throw new RegraNegocioException("Já existe um local com esse nome."); });
        local.atualizar(nome, descricao);
        local.definirModalidades(buscarModalidades(modalidadeIds));
        if (ativo != null) local.definirAtivo(ativo);
        return local;
    }

    @Transactional
    public void excluirLocal(UUID localId) {
        LocalPartida local = locais.findById(localId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
        if (partidas.existsByLocal_Id(localId)) {
            throw new RegraNegocioException("O local não pode ser excluído porque está vinculado a uma ou mais partidas.");
        }
        locais.delete(local);
    }

    @Transactional
    public Categoria criarCategoria(String nome, Integer peso) {
        categorias.findByNomeIgnoreCase(nome.trim()).ifPresent(item -> { throw new RegraNegocioException("Já existe uma categoria com esse nome."); });
        return categorias.save(new Categoria(nome, peso));
    }

    @Transactional
    public Categoria editarCategoria(Long categoriaId, String nome, Integer peso) {
        Categoria categoria = categorias.findById(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
        categorias.findByNomeIgnoreCase(nome.trim())
                .filter(existente -> !existente.getId().equals(categoriaId))
                .ifPresent(item -> { throw new RegraNegocioException("Já existe uma categoria com esse nome."); });
        categoria.atualizar(nome, peso);
        return categoria;
    }

    @Transactional
    public void excluirCategoria(Long categoriaId) {
        Categoria categoria = categorias.findById(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
        if (partidas.existsByCategoria_Id(categoriaId) || jogadores.existsByCategoria_Id(categoriaId)) {
            throw new RegraNegocioException("A categoria não pode ser excluída porque está vinculada a jogadores ou partidas.");
        }
        categorias.delete(categoria);
    }
}

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

    @Transactional
    public Modalidade criarModalidade(String nome) {
        modalidades.findByNomeIgnoreCase(nome.trim()).ifPresent(item -> { throw new RegraNegocioException("Já existe uma modalidade com esse nome."); });
        return modalidades.save(new Modalidade(nome));
    }

    @Transactional
    public Modalidade editarModalidade(UUID modalidadeId, String nome) {
        Modalidade modalidade = modalidades.findById(modalidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modalidade não encontrada."));
        modalidades.findByNomeIgnoreCase(nome.trim())
                .filter(existente -> !existente.getId().equals(modalidadeId))
                .ifPresent(item -> { throw new RegraNegocioException("Já existe uma modalidade com esse nome."); });
        modalidade.atualizarNome(nome);
        return modalidade;
    }

    @Transactional
    public void excluirModalidade(UUID modalidadeId) {
        Modalidade modalidade = modalidades.findById(modalidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modalidade não encontrada."));
        if (partidas.existsByModalidade_Id(modalidadeId)) {
            throw new RegraNegocioException("A modalidade não pode ser excluída porque está vinculada a uma ou mais partidas.");
        }
        modalidades.delete(modalidade);
    }

    @Transactional
    public LocalPartida criarLocal(String nome, String descricao) {
        locais.findByNomeIgnoreCase(nome.trim()).ifPresent(item -> { throw new RegraNegocioException("Já existe um local com esse nome."); });
        return locais.save(new LocalPartida(nome, descricao));
    }

    @Transactional
    public LocalPartida editarLocal(UUID localId, String nome, String descricao) {
        LocalPartida local = locais.findById(localId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
        locais.findByNomeIgnoreCase(nome.trim())
                .filter(existente -> !existente.getId().equals(localId))
                .ifPresent(item -> { throw new RegraNegocioException("Já existe um local com esse nome."); });
        local.atualizar(nome, descricao);
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

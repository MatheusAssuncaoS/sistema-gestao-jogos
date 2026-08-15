package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.dto.*;
import br.com.matheusassuncao.gestaojogos.servico.AdminConfiguracaoService;
import br.com.matheusassuncao.gestaojogos.servico.PartidaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/configuracoes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminConfiguracaoController {
    private final PartidaService partidas;
    private final AdminConfiguracaoService configuracoes;

    public AdminConfiguracaoController(PartidaService partidas, AdminConfiguracaoService configuracoes) {
        this.partidas = partidas;
        this.configuracoes = configuracoes;
    }

    @GetMapping("/modalidades") public List<ModalidadeResponse> modalidades() { return partidas.listarModalidadesAtivas(); }
    @PostMapping("/modalidades") public ResponseEntity<ModalidadeResponse> criarModalidade(@RequestBody @Valid CriarModalidadeRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ModalidadeResponse.de(configuracoes.criarModalidade(request.nome()))); }
    @PutMapping("/modalidades/{modalidadeId}") public ModalidadeResponse editarModalidade(@PathVariable UUID modalidadeId, @RequestBody @Valid CriarModalidadeRequest request) { return ModalidadeResponse.de(configuracoes.editarModalidade(modalidadeId, request.nome())); }
    @DeleteMapping("/modalidades/{modalidadeId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void excluirModalidade(@PathVariable UUID modalidadeId) { configuracoes.excluirModalidade(modalidadeId); }
    @GetMapping("/locais") public List<LocalPartidaResponse> locais() { return partidas.listarLocaisAtivos(); }
    @PostMapping("/locais") public ResponseEntity<LocalPartidaResponse> criarLocal(@RequestBody @Valid CriarLocalPartidaRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(LocalPartidaResponse.de(configuracoes.criarLocal(request.nome(), request.descricao()))); }
    @PutMapping("/locais/{localId}") public LocalPartidaResponse editarLocal(@PathVariable UUID localId, @RequestBody @Valid CriarLocalPartidaRequest request) { return LocalPartidaResponse.de(configuracoes.editarLocal(localId, request.nome(), request.descricao())); }
    @DeleteMapping("/locais/{localId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void excluirLocal(@PathVariable UUID localId) { configuracoes.excluirLocal(localId); }
    @GetMapping("/categorias") public List<CategoriaResponse> categorias() { return partidas.listarCategoriasAtivas(); }
    @PostMapping("/categorias") public ResponseEntity<CategoriaResponse> criarCategoria(@RequestBody @Valid CriarCategoriaRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaResponse.de(configuracoes.criarCategoria(request.nome(), request.peso()))); }
    @PutMapping("/categorias/{categoriaId}") public CategoriaResponse editarCategoria(@PathVariable Long categoriaId, @RequestBody @Valid CriarCategoriaRequest request) { return CategoriaResponse.de(configuracoes.editarCategoria(categoriaId, request.nome(), request.peso())); }
    @DeleteMapping("/categorias/{categoriaId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void excluirCategoria(@PathVariable Long categoriaId) { configuracoes.excluirCategoria(categoriaId); }
}

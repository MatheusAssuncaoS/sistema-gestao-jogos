package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.Categoria;
import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.Partida;
import br.com.matheusassuncao.gestaojogos.dominio.SituacaoAssociativa;
import br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao;
import br.com.matheusassuncao.gestaojogos.dominio.StatusPartida;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.CriarPartidaRequest;
import br.com.matheusassuncao.gestaojogos.repositorio.CategoriaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.InscricaoRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.JogadorRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.LocalPartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.ModalidadeRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import br.com.matheusassuncao.gestaojogos.servico.CalendarioService;
import br.com.matheusassuncao.gestaojogos.servico.PartidaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InscricaoControllerTest extends IntegracaoTest {

    private static final String SENHA = "senha12345";
    private static final String EMAIL_JOGADOR = "jogador.inscricao@teste.com";
    private static final String EMAIL_ORGANIZADOR = "organizador.inscricao@clube.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PartidaService partidaService;

    @Autowired
    private CalendarioService calendarioService;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ModalidadeRepository modalidadeRepository;

    @Autowired
    private LocalPartidaRepository localPartidaRepository;

    @Autowired
    private PapelRepository papelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("UC06: jogador regular se inscreve em partida aberta")
    void inscricaoBemSucedida() throws Exception {
        criarJogadorRegular();
        UUID partidaId = criarPartidaAberta();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.partidaId").value(partidaId.toString()))
                .andExpect(jsonPath("$.equipe").doesNotExist());

        assertThat(inscricaoRepository.contarConfirmados(partidaId)).isEqualTo(1);
    }

    @Test
    @DisplayName("RN14: inscrição duplicada na mesma partida devolve 409")
    void inscricaoDuplicada() throws Exception {
        criarJogadorRegular();
        UUID partidaId = criarPartidaAberta();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("UC07: cancelar libera a vaga")
    void cancelamentoLiberaVaga() throws Exception {
        criarJogadorRegular();
        UUID partidaId = criarPartidaAberta();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isNoContent());

        assertThat(inscricaoRepository.contarConfirmados(partidaId))
                .as("a vaga volta a ficar livre")
                .isZero();
    }

    @Test
    @DisplayName("RN14: após cancelar, o jogador pode se inscrever de novo")
    void reinscricaoAposCancelamento() throws Exception {
        criarJogadorRegular();
        UUID partidaId = criarPartidaAberta();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isCreated());

        assertThat(inscricaoRepository.findAll())
                .as("o histórico preserva a inscrição cancelada e a nova")
                .hasSize(2);
    }

    @Test
    @DisplayName("Cancelar sem inscrição ativa devolve 404")
    void cancelarSemInscricao() throws Exception {
        criarJogadorRegular();
        UUID partidaId = criarPartidaAberta();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(delete("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RN04: partida lotada recusa novas inscrições")
    void partidaLotada() throws Exception {
        UUID partidaId = criarPartidaAberta(1);

        criarJogadorRegular("primeiro@teste.com");
        criarJogadorRegular("segundo@teste.com");

        MockHttpSession primeiro = autenticar("primeiro@teste.com");
        MockHttpSession segundo = autenticar("segundo@teste.com");

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(primeiro))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(segundo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value(org.hamcrest.Matchers.containsString("limite")));

        Partida partida = partidaRepository.findById(partidaId).orElseThrow();

        assertThat(partida.getStatus()).isEqualTo(StatusPartida.LOTADA);
    }

    @Test
    @DisplayName("RN15: cancelamento reabre a partida lotada")
    void cancelamentoReabrePartida() throws Exception {
        UUID partidaId = criarPartidaAberta(1);

        criarJogadorRegular();
        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isCreated());

        assertThat(partidaRepository.findById(partidaId).orElseThrow().getStatus())
                .isEqualTo(StatusPartida.LOTADA);

        mockMvc.perform(delete("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isNoContent());

        assertThat(partidaRepository.findById(partidaId).orElseThrow().getStatus())
                .as("a vaga liberada devolve a partida ao estado aberta")
                .isEqualTo(StatusPartida.ABERTA);
    }

    @Test
    @DisplayName("Partida em rascunho não aceita inscrição: 409")
    void partidaEmRascunho() throws Exception {
        criarJogadorRegular();
        UUID partidaId = criarPartida();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("RN01: jogador irregular não se inscreve")
    void jogadorIrregular() throws Exception {
        criarJogador(EMAIL_JOGADOR, SituacaoAssociativa.IRREGULAR);
        UUID partidaId = criarPartidaAberta();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value(org.hamcrest.Matchers.containsString("regular")));
    }

    @Test
    @DisplayName("Usuário com papel de jogador mas sem cadastro aprovado: 409")
    void usuarioSemPerfilDeJogador() throws Exception {
        criarUsuario(EMAIL_JOGADOR, "JOGADOR");
        UUID partidaId = criarPartidaAberta();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value(org.hamcrest.Matchers.containsString("aprovado")));
    }

    @Test
    @DisplayName("Cancelar a partida cancela as inscrições ativas")
    void cancelamentoDaPartidaAfetaInscricoes() throws Exception {
        criarJogadorRegular();
        UUID partidaId = criarPartidaAberta();

        MockHttpSession jogador = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(jogador))
                .andExpect(status().isCreated());

        MockHttpSession organizador = autenticar(EMAIL_ORGANIZADOR);

        mockMvc.perform(post("/api/organizador/partidas/{id}/cancelar", partidaId)
                        .session(organizador))
                .andExpect(status().isOk());

        assertThat(inscricaoRepository.findAll())
                .singleElement()
                .satisfies(inscricao -> {
                    assertThat(inscricao.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
                    assertThat(inscricao.getMotivoCancelamento())
                            .contains("Partida cancelada");
                });
    }

    @Test
    @DisplayName("UC05: a listagem mostra apenas partidas abertas e futuras")
    void listagemDePartidasDisponiveis() throws Exception {
        criarJogadorRegular();
        criarPartidaAberta();
        criarPartida();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(get("/api/partidas").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ABERTA"));
    }

    @Test
    @DisplayName("UC08: o jogador consulta suas inscrições")
    void minhasInscricoes() throws Exception {
        criarJogadorRegular();
        UUID partidaId = criarPartidaAberta();

        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/partidas/minhas-inscricoes").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("UC17: o organizador consulta os inscritos")
    void organizadorConsultaInscritos() throws Exception {
        criarJogadorRegular();
        UUID partidaId = criarPartidaAberta();

        MockHttpSession jogador = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(jogador))
                .andExpect(status().isCreated());

        MockHttpSession organizador = autenticar(EMAIL_ORGANIZADOR);

        mockMvc.perform(get("/api/organizador/partidas/{id}/inscritos", partidaId)
                        .session(organizador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("Organizador sem papel de jogador não se inscreve: 403")
    void organizadorNaoSeInscreve() throws Exception {
        UUID partidaId = criarPartidaAberta();

        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId).session(sessao))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Inscrição exige autenticação: 401")
    void exigeAutenticacao() throws Exception {
        UUID partidaId = criarPartidaAberta();

        mockMvc.perform(post("/api/partidas/{id}/inscricao", partidaId))
                .andExpect(status().isUnauthorized());
    }

    private UUID criarPartida() {
        return criarPartida(16);
    }

    private UUID criarPartida(int capacidade) {
        Usuario organizador = usuarioRepository.findByEmailIgnoreCase(EMAIL_ORGANIZADOR)
                .orElseGet(() -> criarUsuario(EMAIL_ORGANIZADOR, "ORGANIZADOR"));

        CriarPartidaRequest request = new CriarPartidaRequest(
                modalidadeRepository.findByNomeIgnoreCase("Futebol").orElseThrow().getId(),
                localPartidaRepository.findByAtivoTrue().getFirst().getId(),
                null,
                calendarioService.listarProximosHorarios(30).getFirst(),
                capacidade,
                null,
                null
        );

        return partidaService.criar(request, organizador.getEmail()).id();
    }

    private UUID criarPartidaAberta() {
        return criarPartidaAberta(16);
    }

    private UUID criarPartidaAberta(int capacidade) {
        UUID partidaId = criarPartida(capacidade);
        partidaService.abrir(partidaId);

        return partidaId;
    }

    private Usuario criarJogadorRegular() {
        return criarJogador(EMAIL_JOGADOR, SituacaoAssociativa.REGULAR);
    }

    private Usuario criarJogadorRegular(String email) {
        return criarJogador(email, SituacaoAssociativa.REGULAR);
    }

    private Usuario criarJogador(String email, SituacaoAssociativa situacao) {
        Usuario usuario = criarUsuario(email, "JOGADOR");
        Categoria categoria = categoriaRepository.findByAtivoTrue().getFirst();

        jogadorRepository.save(new Jogador(usuario, null, categoria, situacao, usuario));

        return usuario;
    }

    private Usuario criarUsuario(String email, String papel) {
        Usuario usuario = new Usuario(
                "Usuário " + email,
                email,
                passwordEncoder.encode(SENHA)
        );

        usuario.ativar();
        usuario.adicionarPapel(papelRepository.findByNome(papel).orElseThrow());

        return usuarioRepository.save(usuario);
    }

    private MockHttpSession autenticar(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);

        assertThat(sessao).as("o login deve criar uma sessão").isNotNull();

        return sessao;
    }
}

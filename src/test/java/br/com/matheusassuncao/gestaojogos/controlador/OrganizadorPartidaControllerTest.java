package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.Equipe;
import br.com.matheusassuncao.gestaojogos.dominio.LocalPartida;
import br.com.matheusassuncao.gestaojogos.dominio.Modalidade;
import br.com.matheusassuncao.gestaojogos.dominio.Partida;
import br.com.matheusassuncao.gestaojogos.dominio.StatusPartida;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.EquipeRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.LocalPartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.ModalidadeRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import br.com.matheusassuncao.gestaojogos.servico.CalendarioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizadorPartidaControllerTest extends IntegracaoTest {

    private static final String SENHA = "senha12345";
    private static final String EMAIL_ORGANIZADOR = "organizador@clube.local";
    private static final String EMAIL_JOGADOR = "jogador@teste.com";
    private static final String EMAIL_ADMIN = "admin.teste@clube.local";
    private static final ZoneId FUSO_DO_CLUBE = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalendarioService calendarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PapelRepository papelRepository;

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private EquipeRepository equipeRepository;

    @Autowired
    private ModalidadeRepository modalidadeRepository;

    @Autowired
    private LocalPartidaRepository localPartidaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID modalidadeId;
    private UUID localId;

    @BeforeEach
    void carregarDadosDeReferencia() {
        modalidadeId = modalidadeRepository.findByNomeIgnoreCase("Futebol").orElseThrow().getId();
        localId = localPartidaRepository.findByAtivoTrue().getFirst().getId();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void rejeitarLocalSemVinculoComModalidade() throws Exception {
        criarOrganizador();
        var outra = modalidadeRepository.save(new Modalidade("Sinuca de teste"));
        mockMvc.perform(post("/api/organizador/partidas")
                .session(autenticar(EMAIL_ORGANIZADOR)).contentType(MediaType.APPLICATION_JSON)
                .content(corpoDeCriacao(proximaDataValida()).replace(modalidadeId.toString(), outra.getId().toString())))
                .andExpect(status().isConflict());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void localCompartilhadoMantemConflitoEntreModalidades() throws Exception {
        criarOrganizador();
        var outra = modalidadeRepository.save(new Modalidade("Outra modalidade de teste"));
        var local = localPartidaRepository.findById(localId).orElseThrow();
        local.getModalidades().add(outra);
        localPartidaRepository.saveAndFlush(local);
        var sessao = autenticar(EMAIL_ORGANIZADOR);
        var corpo = corpoDeCriacao(proximaDataValida());
        mockMvc.perform(post("/api/organizador/partidas").session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/organizador/partidas").session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content(corpo.replace(modalidadeId.toString(), outra.getId().toString())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("UC11: partida é criada em RASCUNHO já com as duas equipes")
    void criarPartida() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        MvcResult resultado = mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(proximaDataValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.modalidade").value("Futebol"))
                .andExpect(jsonPath("$.capacidade").value(16))
                .andExpect(jsonPath("$.equipes.length()").value(2))
                .andReturn();

        UUID partidaId = extrairId(resultado);

        assertThat(equipeRepository.findByPartidaId(partidaId))
                .as("as equipes nascem com a partida")
                .hasSize(2)
                .extracting(Equipe::getCor)
                .containsExactlyInAnyOrder(Equipe.COR_AZUL, Equipe.COR_AMARELO);

        assertThat(equipeRepository.findByPartidaId(partidaId))
                .extracting(Equipe::getCapacidade)
                .containsOnly(8);
    }

    @Test
    @DisplayName("Não permite duas partidas no mesmo local, dia e horário")
    void rejeitarHorarioJaOcupado() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);
        OffsetDateTime inicio = proximaDataValida();

        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(inicio)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(inicio)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma partida marcada neste local, dia e horário."));
    }

    @Test
    @DisplayName("Lote é rejeitado por inteiro quando contém horário já ocupado")
    void rejeitarLoteComHorarioOcupado() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);
        OffsetDateTime ocupado = proximaDataValida();
        OffsetDateTime livre = ocupado.plusWeeks(1);

        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(ocupado)))
                .andExpect(status().isCreated());
        long quantidadeAntes = partidaRepository.count();

        String lote = """
                {
                  "modalidadeId": "%s",
                  "localId": "%s",
                  "inicios": ["%s", "%s"]
                }
                """.formatted(modalidadeId, localId, ocupado, livre);

        mockMvc.perform(post("/api/organizador/partidas/lote")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lote))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma partida marcada neste local, dia e horário."));

        assertThat(partidaRepository.count()).isEqualTo(quantidadeAntes);
    }

    @Test
    @DisplayName("Partida no passado é rejeitada: 400")
    void criarPartidaNoPassado() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(OffsetDateTime.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.inicio").exists());
    }

    @Test
    @DisplayName("Inscrições não podem encerrar depois do início da partida: 409")
    void periodoDeInscricaoInvalido() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        OffsetDateTime inicio = proximaDataValida();

        String corpo = """
                {
                  "modalidadeId": "%s",
                  "localId": "%s",
                  "inicio": "%s",
                  "inscricoesAbremEm": "%s",
                  "inscricoesEncerramEm": "%s"
                }
                """.formatted(
                modalidadeId,
                localId,
                inicio,
                OffsetDateTime.now().plusDays(1),
                inicio.plusHours(1)
        );

        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Modalidade inexistente devolve 404")
    void modalidadeInexistente() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        String corpo = """
                {
                  "modalidadeId": "%s",
                  "localId": "%s",
                  "inicio": "%s"
                }
                """.formatted(UUID.randomUUID(), localId, proximaDataValida());

        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Partida aceita dia e horário sem cadastro prévio")
    void partidaForaDoCalendario() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        // Dia e horário são definidos diretamente na partida.
        OffsetDateTime foraDoCalendario = OffsetDateTime.now()
                .atZoneSameInstant(FUSO_DO_CLUBE)
                .toLocalDate()
                .plusDays(1)
                .with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.TUESDAY))
                .atTime(LocalTime.of(3, 0))
                .atZone(FUSO_DO_CLUBE)
                .toOffsetDateTime();

        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(foraDoCalendario)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Edição com a versão correta altera a partida e incrementa a versão")
    void editarPartida() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        UUID partidaId = criarPartidaViaApi(sessao);

        OffsetDateTime novoInicio = outraDataValida();

        mockMvc.perform(put("/api/organizador/partidas/{id}", partidaId)
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeEdicao(novoInicio, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(1));
    }

    @Test
    @DisplayName("Lock otimista: editar com versão desatualizada devolve 409")
    void edicaoConcorrenteDevolveConflito() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        UUID partidaId = criarPartidaViaApi(sessao);

        // primeira edição: a versão passa de 0 para 1
        mockMvc.perform(put("/api/organizador/partidas/{id}", partidaId)
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeEdicao(outraDataValida(), 0)))
                .andExpect(status().isOk());

        // segunda edição com a versão antiga: simula alguém que carregou a
        // tela antes da primeira alteração
        mockMvc.perform(put("/api/organizador/partidas/{id}", partidaId)
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeEdicao(outraDataValida(), 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de edição"));
    }

    @Test
    @DisplayName("Abrir muda o status de RASCUNHO para ABERTA")
    void abrirPartida() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        UUID partidaId = criarPartidaViaApi(sessao);

        mockMvc.perform(post("/api/organizador/partidas/{id}/abrir", partidaId)
                        .session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    @DisplayName("Abrir uma partida já aberta devolve 409")
    void abrirPartidaJaAberta() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        UUID partidaId = criarPartidaViaApi(sessao);

        mockMvc.perform(post("/api/organizador/partidas/{id}/abrir", partidaId).session(sessao))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/organizador/partidas/{id}/abrir", partidaId).session(sessao))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cancelar muda o status para CANCELADA")
    void cancelarPartida() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        UUID partidaId = criarPartidaViaApi(sessao);

        mockMvc.perform(post("/api/organizador/partidas/{id}/cancelar", partidaId)
                        .session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        Partida partida = partidaRepository.findById(partidaId).orElseThrow();

        assertThat(partida.getStatus()).isEqualTo(StatusPartida.CANCELADA);
    }

    @Test
    @DisplayName("Partida cancelada não pode ser editada: 409")
    void editarPartidaCancelada() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        UUID partidaId = criarPartidaViaApi(sessao);

        mockMvc.perform(post("/api/organizador/partidas/{id}/cancelar", partidaId).session(sessao))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/organizador/partidas/{id}", partidaId)
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeEdicao(outraDataValida(), 1)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cancelar duas vezes devolve 409")
    void cancelarPartidaJaCancelada() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        UUID partidaId = criarPartidaViaApi(sessao);

        mockMvc.perform(post("/api/organizador/partidas/{id}/cancelar", partidaId).session(sessao))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/organizador/partidas/{id}/cancelar", partidaId).session(sessao))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A listagem traz as partidas futuras")
    void listarPartidas() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        criarPartidaViaApi(sessao);
        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(outraDataValida())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/organizador/partidas").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Partida inexistente devolve 404")
    void partidaInexistente() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        mockMvc.perform(get("/api/organizador/partidas/{id}", UUID.randomUUID())
                        .session(sessao))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Lista as modalidades ativas, dado de referência do formulário de criação")
    void listarModalidades() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        mockMvc.perform(get("/api/organizador/partidas/modalidades").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", org.hamcrest.Matchers.hasItem("Futebol")));
    }

    @Test
    @DisplayName("Lista os locais ativos, dado de referência do formulário de criação")
    void listarLocais() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        mockMvc.perform(get("/api/organizador/partidas/locais").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", org.hamcrest.Matchers.hasItem("Campo principal")));
    }

    @Test
    @DisplayName("Lista as categorias ativas, dado de referência do formulário de criação")
    void listarCategoriasDeReferencia() throws Exception {
        criarOrganizador();
        MockHttpSession sessao = autenticar(EMAIL_ORGANIZADOR);

        mockMvc.perform(get("/api/organizador/partidas/categorias").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Jogador não acessa os dados de referência de partidas: 403")
    void jogadorNaoAcessaReferenciasDePartida() throws Exception {
        criarJogador();
        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(get("/api/organizador/partidas/modalidades").session(sessao))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrador acompanha as partidas: lista, detalha e vê inscritos")
    void administradorVePartidas() throws Exception {
        criarOrganizador();
        UUID partidaId = criarPartidaViaApi(autenticar(EMAIL_ORGANIZADOR));

        criarAdministrador();
        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(get("/api/organizador/partidas").session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/organizador/partidas/{id}", partidaId).session(sessaoAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/organizador/partidas/{id}/inscritos", partidaId).session(sessaoAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Administrador cria, abre e cancela partidas")
    void administradorCriaPartida() throws Exception {
        criarOrganizador();
        UUID partidaId = criarPartidaViaApi(autenticar(EMAIL_ORGANIZADOR));

        criarAdministrador();
        MockHttpSession sessaoAdmin = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessaoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(outraDataValida())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/organizador/partidas/{id}/abrir", partidaId).session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"));

        mockMvc.perform(post("/api/organizador/partidas/{id}/abrir", partidaId).session(sessaoAdmin))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/organizador/partidas/{id}/cancelar", partidaId).session(sessaoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        mockMvc.perform(get("/api/organizador/partidas/modalidades").session(sessaoAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void exclusaoLogicaMantemPartidaNoHistoricoELiberaHorario() throws Exception {
        criarAdministrador();
        MockHttpSession sessao = autenticar(EMAIL_ADMIN);
        OffsetDateTime inicio = proximaDataValida();
        UUID partidaId = extrairId(mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(inicio)))
                .andExpect(status().isCreated()).andReturn());

        mockMvc.perform(post("/api/organizador/partidas/{id}/excluir", partidaId).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCLUIDA"));
        mockMvc.perform(get("/api/organizador/partidas").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].status".formatted(partidaId)).value("EXCLUIDA"));
        mockMvc.perform(post("/api/organizador/partidas").session(sessao)
                        .contentType(MediaType.APPLICATION_JSON).content(corpoDeCriacao(inicio)))
                .andExpect(status().isCreated());
    }

    @Test
    void somenteRascunhoPodeSerExcluido() throws Exception {
        criarAdministrador();
        MockHttpSession sessao = autenticar(EMAIL_ADMIN);
        UUID partidaId = criarPartidaViaApi(sessao);
        mockMvc.perform(post("/api/organizador/partidas/{id}/abrir", partidaId).session(sessao))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/organizador/partidas/{id}/excluir", partidaId).session(sessao))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Jogador não gerencia partidas: 403")
    void jogadorNaoGerenciaPartidas() throws Exception {
        criarJogador();
        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(get("/api/organizador/partidas").session(sessao))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(proximaDataValida())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Gestão de partidas exige autenticação: 401")
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/organizador/partidas"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Primeira data futura que satisfaz o calendário do clube.
     *
     * Calcular em vez de fixar um deslocamento deixa o teste independente do
     * dia da semana em que ele roda.
     */
    private OffsetDateTime proximaDataValida() {
        return calendarioService.listarProximosHorarios(30).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "O calendário padrão da migration V5 deveria oferecer horários."
                ));
    }

    /**
     * Segunda data válida, para os testes de edição que mudam o horário.
     */
    private OffsetDateTime outraDataValida() {
        return calendarioService.listarProximosHorarios(30).stream()
                .skip(1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "O calendário padrão deveria oferecer mais de um horário."
                ));
    }

    @Test
    void criaAbertaQuandoInscricoesJaEstaoNoPrazo() throws Exception {
        criarOrganizador();
        var agora = OffsetDateTime.now();
        mockMvc.perform(post("/api/organizador/partidas").session(autenticar(EMAIL_ORGANIZADOR))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"modalidadeId":"%s","localId":"%s","inicio":"%s",
                 "inscricoesAbremEm":"%s","inscricoesEncerramEm":"%s"}
                """.formatted(modalidadeId, localId, agora.plusDays(2), agora.minusMinutes(1), agora.plusDays(1))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    void editarRascunhoAbreInscricoesQuandoPrazoJaComecou() throws Exception {
        criarOrganizador();
        var sessao = autenticar(EMAIL_ORGANIZADOR);
        var id = criarPartidaViaApi(sessao);
        var partida = partidaRepository.findById(id).orElseThrow();
        mockMvc.perform(put("/api/organizador/partidas/{id}", id).session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"localId":"%s","inicio":"%s","versao":%d,
                 "inscricoesAbremEm":"%s"}
                """.formatted(localId, partida.getInicio(), partida.getVersao(), OffsetDateTime.now().minusMinutes(1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    void rotinaAbreNoPrazoUmaVezEAtualizaVersao() throws Exception {
        criarOrganizador();
        var sessao = autenticar(EMAIL_ORGANIZADOR);
        var abre = OffsetDateTime.now().plusDays(1).withNano(0);
        var resultado = mockMvc.perform(post("/api/organizador/partidas").session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"modalidadeId":"%s","localId":"%s","inicio":"%s",
                 "inscricoesAbremEm":"%s","inscricoesEncerramEm":"%s"}
                """.formatted(modalidadeId, localId, abre.plusDays(2), abre, abre.plusDays(1))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RASCUNHO")).andReturn();
        var id = extrairId(resultado);
        var versao = partidaRepository.findById(id).orElseThrow().getVersao();
        assertThat(partidaRepository.abrirInscricoesAgendadas(abre.minusSeconds(1))).isZero();
        assertThat(partidaRepository.abrirInscricoesAgendadas(abre)).isEqualTo(1);
        var atualizada = partidaRepository.findById(id).orElseThrow();
        assertThat(atualizada.getStatus().name()).isEqualTo("ABERTA");
        assertThat(atualizada.getVersao()).isEqualTo(versao + 1);
        assertThat(partidaRepository.abrirInscricoesAgendadas(abre)).isZero();
    }

    @Test
    void duracaoReservaTodoOIntervaloEAceitaPartidaNoTerminoExato() throws Exception {
        criarOrganizador();
        var sessao = autenticar(EMAIL_ORGANIZADOR);
        var inicio = proximaDataValida();
        mockMvc.perform(post("/api/organizador/partidas").session(sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDeCriacao(inicio).replace("}", ", \"duracaoMinutos\": 90}")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.duracaoMinutos").value(90));
        mockMvc.perform(post("/api/organizador/partidas").session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content(corpoDeCriacao(inicio.plusMinutes(30))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/organizador/partidas").session(sessao)
                .contentType(MediaType.APPLICATION_JSON).content(corpoDeCriacao(inicio.plusMinutes(90))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.duracaoMinutos").value(60));
    }

    @Test
    void permiteEditarDuracaoDaPartida() throws Exception {
        criarOrganizador();
        var sessao = autenticar(EMAIL_ORGANIZADOR);
        var id = criarPartidaViaApi(sessao);
        var partida = partidaRepository.findById(id).orElseThrow();
        mockMvc.perform(put("/api/organizador/partidas/{id}", id).session(sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDeEdicao(partida.getInicio(), partida.getVersao()).replace("}", ", \"duracaoMinutos\": 75}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duracaoMinutos").value(75));
        assertThat(partidaRepository.findById(id).orElseThrow().getDuracaoMinutos()).isEqualTo(75);
    }

    @Test
    void duracaoInvalidaEhRejeitada() throws Exception {
        criarOrganizador();
        var sessao = autenticar(EMAIL_ORGANIZADOR);
        for (int duracao : new int[]{0, -1, 1441}) {
            mockMvc.perform(post("/api/organizador/partidas").session(sessao)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpoDeCriacao(proximaDataValida()).replace("}", ", \"duracaoMinutos\": " + duracao + "}")))
                    .andExpect(status().isBadRequest());
        }
    }

    private UUID criarPartidaViaApi(MockHttpSession sessao) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/organizador/partidas")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao(proximaDataValida())))
                .andExpect(status().isCreated())
                .andReturn();

        return extrairId(resultado);
    }

    private UUID extrairId(MvcResult resultado) throws Exception {
        String id = com.jayway.jsonpath.JsonPath.read(
                resultado.getResponse().getContentAsString(),
                "$.id"
        );

        return UUID.fromString(id);
    }

    private String corpoDeCriacao(OffsetDateTime inicio) {
        return """
                {
                  "modalidadeId": "%s",
                  "localId": "%s",
                  "inicio": "%s"
                }
                """.formatted(modalidadeId, localId, inicio);
    }

    private String corpoDeEdicao(OffsetDateTime inicio, int versao) {
        return """
                {
                  "localId": "%s",
                  "inicio": "%s",
                  "versao": %d
                }
                """.formatted(localId, inicio, versao);
    }

    private Usuario criarAdministrador() {
        Usuario admin = new Usuario(
                "Administrador de Teste",
                EMAIL_ADMIN,
                passwordEncoder.encode(SENHA)
        );

        admin.ativar();
        admin.adicionarPapel(papelRepository.findByNome("ADMINISTRADOR").orElseThrow());

        return usuarioRepository.save(admin);
    }

    private Usuario criarOrganizador() {
        Usuario organizador = new Usuario(
                "Organizador de Teste",
                EMAIL_ORGANIZADOR,
                passwordEncoder.encode(SENHA)
        );

        organizador.ativar();
        organizador.adicionarPapel(papelRepository.findByNome("ORGANIZADOR").orElseThrow());

        return usuarioRepository.save(organizador);
    }

    private Usuario criarJogador() {
        Usuario jogador = new Usuario(
                "Jogador de Teste",
                EMAIL_JOGADOR,
                passwordEncoder.encode(SENHA)
        );

        jogador.ativar();
        jogador.adicionarPapel(papelRepository.findByNome("JOGADOR").orElseThrow());

        return usuarioRepository.save(jogador);
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

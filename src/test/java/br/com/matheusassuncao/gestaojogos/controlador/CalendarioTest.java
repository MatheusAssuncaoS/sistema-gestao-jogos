package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.TipoExcecao;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.DiaFuncionamentoRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.ExcecaoCalendarioRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import br.com.matheusassuncao.gestaojogos.servico.CalendarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalendarioTest extends IntegracaoTest {

    private static final String SENHA = "senha12345";
    private static final String EMAIL_ADMIN = "admin.calendario@clube.local";
    private static final String EMAIL_JOGADOR = "jogador.calendario@teste.com";
    private static final ZoneId FUSO_DO_CLUBE = ZoneId.of("America/Sao_Paulo");

    /**
     * Dias configurados pelo seed da migration V5. Qualquer outro dia de
     * funcionamento presente na base foi criado por um teste (ex.:
     * administradorAdicionaDia) e precisa ser limpo, senão vaza para os
     * testes seguintes.
     */
    private static final Set<DayOfWeek> DIAS_DO_SEED =
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalendarioService calendarioService;

    @Autowired
    private ExcecaoCalendarioRepository excecaoRepository;

    @Autowired
    private DiaFuncionamentoRepository diaFuncionamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PapelRepository papelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * O TRUNCATE da base não limpa o calendário, que é dado de referência,
     * então as exceções e os dias de funcionamento criados aqui precisam ser
     * removidos explicitamente, ou vazam para os testes seguintes.
     */
    @AfterEach
    void limparCalendario() {
        excecaoRepository.deleteAll();

        diaFuncionamentoRepository.findAll().stream()
                .filter(dia -> !DIAS_DO_SEED.contains(dia.getDiaDaSemana()))
                .forEach(diaFuncionamentoRepository::delete);
    }

    @Test
    @DisplayName("RN06: o seed da migration V5 configura segunda, quarta e sexta")
    void seedDoCalendario() {
        assertThat(calendarioService.listarDiasDeFuncionamento())
                .hasSize(3)
                .extracting("diaDaSemana")
                .containsExactlyInAnyOrder(
                        DayOfWeek.MONDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY
                );
    }

    @Test
    @DisplayName("RN06: horário configurado é aceito")
    void horarioConfiguradoEhValido() {
        OffsetDateTime valida = calendarioService.listarProximosHorarios(30).getFirst();

        assertThatCode(() -> calendarioService.validarDataDisponivel(valida))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RN06: dia fora do calendário é rejeitado")
    void diaForaDoCalendario() {
        OffsetDateTime terca = proximaTerca().atTime(19, 0)
                .atZone(FUSO_DO_CLUBE)
                .toOffsetDateTime();

        assertThatThrownBy(() -> calendarioService.validarDataDisponivel(terca))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não realiza partidas");
    }

    @Test
    @DisplayName("RN06: dia correto em horário não configurado é rejeitado")
    void horarioForaDoCalendario() {
        OffsetDateTime segundaDeMadrugada = proximaSegunda().atTime(3, 0)
                .atZone(FUSO_DO_CLUBE)
                .toOffsetDateTime();

        assertThatThrownBy(() -> calendarioService.validarDataDisponivel(segundaDeMadrugada))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("RN07: feriado bloqueia a data")
    void feriadoBloqueia() {
        OffsetDateTime valida = calendarioService.listarProximosHorarios(30).getFirst();
        LocalDate data = valida.atZoneSameInstant(FUSO_DO_CLUBE).toLocalDate();

        calendarioService.adicionarExcecao("Feriado municipal", TipoExcecao.FERIADO, data, data);

        assertThatThrownBy(() -> calendarioService.validarDataDisponivel(valida))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Feriado municipal");
    }

    @Test
    @DisplayName("RN07: recesso bloqueia todas as datas do período")
    void recessoBloqueiaOPeriodoInteiro() {
        List<OffsetDateTime> horarios = calendarioService.listarProximosHorarios(30);
        OffsetDateTime primeiro = horarios.getFirst();
        LocalDate inicio = primeiro.atZoneSameInstant(FUSO_DO_CLUBE).toLocalDate();

        calendarioService.adicionarExcecao(
                "Recesso de fim de ano",
                TipoExcecao.RECESSO,
                inicio,
                inicio.plusDays(20)
        );

        assertThat(calendarioService.listarProximosHorarios(20))
                .as("nenhum horário deve sobrar dentro do recesso")
                .isEmpty();
    }

    @Test
    @DisplayName("A data seguinte ao fim do recesso volta a ser válida")
    void aposORecessoVoltaAValer() {
        OffsetDateTime primeiro = calendarioService.listarProximosHorarios(30).getFirst();
        LocalDate data = primeiro.atZoneSameInstant(FUSO_DO_CLUBE).toLocalDate();

        calendarioService.adicionarExcecao(
                "Recesso curto",
                TipoExcecao.RECESSO,
                data,
                data
        );

        assertThat(calendarioService.listarProximosHorarios(30))
                .isNotEmpty()
                .allSatisfy(horario -> assertThat(
                        horario.atZoneSameInstant(FUSO_DO_CLUBE).toLocalDate()
                ).isNotEqualTo(data));
    }

    @Test
    @DisplayName("Período com fim anterior ao início é rejeitado")
    void periodoInvalido() {
        LocalDate hoje = LocalDate.now(FUSO_DO_CLUBE);

        assertThatThrownBy(() -> calendarioService.adicionarExcecao(
                "Inválido", TipoExcecao.FERIADO, hoje.plusDays(5), hoje
        )).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("UC21: administrador adiciona um dia de funcionamento")
    void administradorAdicionaDia() throws Exception {
        criarAdministrador();
        MockHttpSession sessao = autenticar(EMAIL_ADMIN);

        mockMvc.perform(post("/api/admin/calendario/dias-funcionamento")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diaDaSemana": "SATURDAY",
                                  "horario": "10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diaDaSemana").value("SATURDAY"));

        assertThat(calendarioService.listarDiasDeFuncionamento()).hasSize(4);
    }

    @Test
    @DisplayName("UC21: administrador cadastra um recesso")
    void administradorCadastraRecesso() throws Exception {
        criarAdministrador();
        MockHttpSession sessao = autenticar(EMAIL_ADMIN);

        LocalDate inicio = LocalDate.now(FUSO_DO_CLUBE).plusMonths(4);

        mockMvc.perform(post("/api/admin/calendario/excecoes")
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descricao": "Recesso de fim de ano",
                                  "tipo": "RECESSO",
                                  "inicio": "%s",
                                  "fim": "%s"
                                }
                                """.formatted(inicio, inicio.plusDays(15))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("RECESSO"));
    }

    @Test
    @DisplayName("Jogador não configura o calendário: 403")
    void jogadorNaoConfiguraCalendario() throws Exception {
        criarJogador();
        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(get("/api/admin/calendario/dias-funcionamento").session(sessao))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Qualquer autenticado consulta os horários disponíveis")
    void jogadorConsultaHorarios() throws Exception {
        criarJogador();
        MockHttpSession sessao = autenticar(EMAIL_JOGADOR);

        mockMvc.perform(get("/api/calendario/horarios-disponiveis").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("A consulta de horários exige autenticação: 401")
    void consultaExigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/calendario/horarios-disponiveis"))
                .andExpect(status().isUnauthorized());
    }

    private LocalDate proximaSegunda() {
        return LocalDate.now(FUSO_DO_CLUBE).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private LocalDate proximaTerca() {
        return LocalDate.now(FUSO_DO_CLUBE).with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
    }

    private Usuario criarAdministrador() {
        Usuario admin = new Usuario(
                "Administrador do Calendário",
                EMAIL_ADMIN,
                passwordEncoder.encode(SENHA)
        );

        admin.ativar();
        admin.adicionarPapel(papelRepository.findByNome("ADMINISTRADOR").orElseThrow());

        return usuarioRepository.save(admin);
    }

    private Usuario criarJogador() {
        Usuario jogador = new Usuario(
                "Jogador do Calendário",
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

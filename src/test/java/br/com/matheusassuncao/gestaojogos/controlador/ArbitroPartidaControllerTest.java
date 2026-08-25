package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dominio.Inscricao;
import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.Partida;
import br.com.matheusassuncao.gestaojogos.dominio.SituacaoAssociativa;
import br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao;
import br.com.matheusassuncao.gestaojogos.repositorio.CategoriaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.InscricaoRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.JogadorRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.LocalPartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.ModalidadeRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArbitroPartidaControllerTest extends IntegracaoTest {

    private static final String EMAIL = "arbitro@clube.local";
    private static final String SENHA = "senha12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PapelRepository papelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired private PartidaRepository partidaRepository;
    @Autowired private ModalidadeRepository modalidadeRepository;
    @Autowired private LocalPartidaRepository localPartidaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private JogadorRepository jogadorRepository;
    @Autowired private InscricaoRepository inscricaoRepository;

    @Test
    @DisplayName("Central do árbitro exige o papel de árbitro")
    void exigePapelDeArbitro() throws Exception {
        Usuario usuario = criarUsuario("ARBITRO");

        mockMvc.perform(get("/api/arbitro/partidas").session(autenticar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        usuario.removerPapel(papelRepository.findByNome("ARBITRO").orElseThrow());
        usuario = usuarioRepository.save(usuario);

        mockMvc.perform(get("/api/arbitro/partidas").session(autenticar()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Central do árbitro exige autenticação")
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/arbitro/partidas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Estado da arbitragem persiste escalação, cronômetro, gols e cartões")
    void persisteEstadoDaArbitragem() throws Exception {
        Usuario arbitro = criarUsuario("ARBITRO");
        Partida partida = criarPartida(arbitro);
        Jogador primeiro = criarJogador("Primeiro Jogador", "primeiro@teste.local", arbitro);
        Jogador segundo = criarJogador("Segundo Jogador", "segundo@teste.local", arbitro);
        inscricaoRepository.save(new Inscricao(partida, primeiro, StatusInscricao.CONFIRMADA));
        inscricaoRepository.save(new Inscricao(partida, segundo, StatusInscricao.CONFIRMADA));
        MockHttpSession sessao = autenticar();

        mockMvc.perform(get("/api/arbitro/partidas/{id}/estado", partida.getId()).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARACAO"))
                .andExpect(jsonPath("$.segundos").value(0))
                .andExpect(jsonPath("$.dados.escala").isEmpty());

        String corpo = """
                {
                  "status": "EM_ANDAMENTO",
                  "segundos": 75,
                  "versao": 0,
                  "dados": {
                    "acrescimos": 3,
                    "escala": {
                      "%s": "AMARELO",
                      "%s": "AZUL"
                    },
                    "gols": [{
                      "id": "gol-1",
                      "time": "AMARELO",
                      "jogadorId": "%s",
                      "segundo": 60
                    }],
                    "punicoes": [{
                      "id": "cartao-1",
                      "time": "AZUL",
                      "jogadorId": "%s",
                      "tipo": "AMARELO",
                      "motivo": "Falta antidesportiva",
                      "segundo": 70
                    }]
                  }
                }
                """.formatted(primeiro.getId(), segundo.getId(), primeiro.getId(), segundo.getId());

        mockMvc.perform(put("/api/arbitro/partidas/{id}/estado", partida.getId())
                        .session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.dados.acrescimos").value(3))
                .andExpect(jsonPath("$.dados.gols[0].jogadorId").value(primeiro.getId().toString()))
                .andExpect(jsonPath("$.dados.punicoes[0].tipo").value("AMARELO"));

        mockMvc.perform(get("/api/arbitro/partidas/{id}/estado", partida.getId()).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segundos").value(org.hamcrest.Matchers.greaterThanOrEqualTo(75)))
                .andExpect(jsonPath("$.dados.escala['%s']".formatted(primeiro.getId())).value("AMARELO"));
    }

    @Test
    @DisplayName("Backend impede time acima da metade dos confirmados")
    void impedeTimeAcimaDoLimite() throws Exception {
        Usuario arbitro = criarUsuario("ARBITRO");
        Partida partida = criarPartida(arbitro);
        Jogador primeiro = criarJogador("Primeiro Jogador", "limite1@teste.local", arbitro);
        Jogador segundo = criarJogador("Segundo Jogador", "limite2@teste.local", arbitro);
        inscricaoRepository.save(new Inscricao(partida, primeiro, StatusInscricao.CONFIRMADA));
        inscricaoRepository.save(new Inscricao(partida, segundo, StatusInscricao.CONFIRMADA));

        mockMvc.perform(put("/api/arbitro/partidas/{id}/estado", partida.getId())
                        .session(autenticar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"PREPARACAO", "segundos":0, "versao":0,
                                  "dados":{"acrescimos":0,"escala":{"%s":"AZUL","%s":"AZUL"},"gols":[],"punicoes":[]}
                                }
                                """.formatted(primeiro.getId(), segundo.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("metade")));
    }

    private Usuario criarUsuario(String papel) {
        Usuario usuario = new Usuario("Árbitro de Teste", EMAIL, passwordEncoder.encode(SENHA));
        usuario.ativar();
        usuario.adicionarPapel(papelRepository.findByNome(papel).orElseThrow());
        return usuarioRepository.save(usuario);
    }

    private Partida criarPartida(Usuario responsavel) {
        Partida partida = new Partida(
                modalidadeRepository.findByNomeIgnoreCase("Futebol").orElseThrow(),
                localPartidaRepository.findByAtivoTrue().getFirst(),
                null,
                OffsetDateTime.now().plusDays(1),
                16,
                null,
                null,
                responsavel
        );
        partida.abrir();
        return partidaRepository.save(partida);
    }

    private Jogador criarJogador(String nome, String email, Usuario aprovador) {
        Usuario usuario = new Usuario(nome, email, passwordEncoder.encode(SENHA));
        usuario.ativar();
        usuario.adicionarPapel(papelRepository.findByNome("JOGADOR").orElseThrow());
        usuario = usuarioRepository.save(usuario);
        return jogadorRepository.save(new Jogador(
                usuario,
                "MAT-" + UUID.randomUUID(),
                categoriaRepository.findAll().getFirst(),
                SituacaoAssociativa.REGULAR,
                aprovador
        ));
    }

    private MockHttpSession autenticar() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "senha": "%s" }
                                """.formatted(EMAIL, SENHA)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        assertThat(sessao).isNotNull();
        return sessao;
    }
}

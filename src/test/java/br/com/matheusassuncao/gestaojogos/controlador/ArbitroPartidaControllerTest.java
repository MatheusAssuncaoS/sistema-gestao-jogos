package br.com.matheusassuncao.gestaojogos.controlador;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.repositorio.PapelRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("Central do árbitro exige o papel de árbitro")
    void exigePapelDeArbitro() throws Exception {
        Usuario usuario = criarUsuario("ARBITRO");

        mockMvc.perform(get("/api/arbitro/partidas").session(autenticar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        usuario.removerPapel(papelRepository.findByNome("ARBITRO").orElseThrow());
        usuarioRepository.save(usuario);

        mockMvc.perform(get("/api/arbitro/partidas").session(autenticar()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Central do árbitro exige autenticação")
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/arbitro/partidas"))
                .andExpect(status().isUnauthorized());
    }

    private Usuario criarUsuario(String papel) {
        Usuario usuario = new Usuario("Árbitro de Teste", EMAIL, passwordEncoder.encode(SENHA));
        usuario.ativar();
        usuario.adicionarPapel(papelRepository.findByNome(papel).orElseThrow());
        return usuarioRepository.save(usuario);
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

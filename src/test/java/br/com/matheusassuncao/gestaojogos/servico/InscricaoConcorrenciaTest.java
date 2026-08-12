package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.IntegracaoTest;
import br.com.matheusassuncao.gestaojogos.dominio.Categoria;
import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.Partida;
import br.com.matheusassuncao.gestaojogos.dominio.SituacaoAssociativa;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O teste mais importante do sistema.
 *
 * A regra de negócio diz que uma partida de futebol tem no máximo dezesseis
 * jogadores de linha (RN02). Validar isso no fluxo normal é trivial; o
 * problema aparece quando duas pessoas clicam em "inscrever-se" no mesmo
 * instante, com uma vaga restante.
 *
 * Sem controle de concorrência, ambas as transações contariam quinze
 * confirmados, ambas concluiriam que há vaga, e a partida terminaria com
 * dezessete. Cada transação, isoladamente, teria feito tudo certo: o erro
 * mora no intervalo entre contar e inserir.
 *
 * Estes testes rodam contra um PostgreSQL real (Testcontainers), porque um
 * banco em memória não reproduz o comportamento de lock que está sendo
 * verificado.
 */
class InscricaoConcorrenciaTest extends IntegracaoTest {

    private static final String SENHA = "senha12345";

    @Autowired
    private InscricaoService inscricaoService;

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
    @DisplayName("RN02: duas inscrições simultâneas na última vaga, apenas uma confirma")
    void duasInscricoesDisputandoAUltimaVaga() throws Exception {
        UUID partidaId = criarPartidaAberta(2);

        // A primeira vaga é ocupada normalmente, sobrando exatamente uma.
        inscricaoService.inscrever(partidaId, criarJogador("ocupante@teste.com").getEmail());

        String primeiro = criarJogador("disputa1@teste.com").getEmail();
        String segundo = criarJogador("disputa2@teste.com").getEmail();

        Resultado resultado = executarEmParalelo(partidaId, List.of(primeiro, segundo));

        assertThat(resultado.sucessos())
                .as("exatamente uma das duas inscrições deve ser confirmada")
                .isEqualTo(1);

        assertThat(resultado.falhas())
                .as("a outra deve ser recusada por falta de vaga")
                .isEqualTo(1);

        assertThat(inscricaoRepository.contarConfirmados(partidaId))
                .as("a capacidade nunca pode ser ultrapassada")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("RN02: vinte inscrições simultâneas em dezesseis vagas")
    void muitasInscricoesSimultaneas() throws Exception {
        UUID partidaId = criarPartidaAberta(16);

        List<String> jogadores = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            jogadores.add(criarJogador("concorrente%d@teste.com".formatted(i)).getEmail());
        }

        Resultado resultado = executarEmParalelo(partidaId, jogadores);

        assertThat(resultado.sucessos()).isEqualTo(16);
        assertThat(resultado.falhas()).isEqualTo(4);

        assertThat(inscricaoRepository.contarConfirmados(partidaId))
                .as("mesmo com vinte disputantes, a partida fecha com dezesseis")
                .isEqualTo(16);

        Partida partida = partidaRepository.findById(partidaId).orElseThrow();

        assertThat(partida.getStatus())
                .as("RN04: ao encher, a partida é marcada como lotada")
                .isEqualTo(StatusPartida.LOTADA);
    }

    @Test
    @DisplayName("RN14: o mesmo jogador clicando duas vezes gera uma única inscrição")
    void mesmoJogadorEmParalelo() throws Exception {
        UUID partidaId = criarPartidaAberta(16);

        String jogador = criarJogador("apressado@teste.com").getEmail();

        Resultado resultado = executarEmParalelo(partidaId, List.of(jogador, jogador));

        assertThat(resultado.sucessos())
                .as("o duplo clique não pode gerar duas inscrições")
                .isEqualTo(1);

        assertThat(inscricaoRepository.contarConfirmados(partidaId)).isEqualTo(1);
    }

    /**
     * Dispara todas as inscrições no mesmo instante.
     *
     * O CountDownLatch é o que garante a simultaneidade real: cada thread
     * prepara tudo e fica bloqueada esperando o sinal, então a disputa começa
     * de fato ao mesmo tempo. Sem isso, as threads iniciariam em sequência e
     * o teste não exercitaria a concorrência.
     */
    private Resultado executarEmParalelo(UUID partidaId, List<String> emails) throws Exception {
        int total = emails.size();

        ExecutorService executor = Executors.newFixedThreadPool(total);
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch chegada = new CountDownLatch(total);

        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhas = new AtomicInteger();

        for (String email : emails) {
            executor.submit(() -> {
                try {
                    largada.await();
                    inscricaoService.inscrever(partidaId, email);
                    sucessos.incrementAndGet();
                } catch (Exception excecao) {
                    falhas.incrementAndGet();
                } finally {
                    chegada.countDown();
                }
            });
        }

        largada.countDown();

        boolean terminou = chegada.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(terminou)
                .as("as inscrições não podem travar: um deadlock seguraria as threads")
                .isTrue();

        return new Resultado(sucessos.get(), falhas.get());
    }

    private UUID criarPartidaAberta(int capacidade) {
        Usuario organizador = criarUsuario("organizador.concorrencia@clube.local", "ORGANIZADOR");

        CriarPartidaRequest request = new CriarPartidaRequest(
                modalidadeRepository.findByNomeIgnoreCase("Futebol").orElseThrow().getId(),
                localPartidaRepository.findByAtivoTrue().getFirst().getId(),
                null,
                calendarioService.listarProximosHorarios(30).getFirst(),
                capacidade,
                null,
                null
        );

        UUID partidaId = partidaService.criar(request, organizador.getEmail()).id();
        partidaService.abrir(partidaId);

        return partidaId;
    }

    private Usuario criarJogador(String email) {
        Usuario usuario = criarUsuario(email, "JOGADOR");
        Categoria categoria = categoriaRepository.findByAtivoTrue().getFirst();

        jogadorRepository.save(new Jogador(
                usuario,
                null,
                categoria,
                SituacaoAssociativa.REGULAR,
                usuario
        ));

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

    private record Resultado(int sucessos, int falhas) {
    }
}

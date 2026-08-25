package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.EstadoArbitragem;
import br.com.matheusassuncao.gestaojogos.dominio.Inscricao;
import br.com.matheusassuncao.gestaojogos.dominio.Partida;
import br.com.matheusassuncao.gestaojogos.dominio.StatusArbitragem;
import br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao;
import br.com.matheusassuncao.gestaojogos.dto.AtualizarEstadoArbitragemRequest;
import br.com.matheusassuncao.gestaojogos.dto.DadosArbitragemRequest;
import br.com.matheusassuncao.gestaojogos.dto.EstadoArbitragemResponse;
import br.com.matheusassuncao.gestaojogos.dto.GolArbitragemRequest;
import br.com.matheusassuncao.gestaojogos.dto.PunicaoArbitragemRequest;
import br.com.matheusassuncao.gestaojogos.dto.ResumoArbitragemResponse;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.EstadoArbitragemRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.InscricaoRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PartidaRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ArbitragemService {

    private static final Set<String> TIMES = Set.of("AMARELO", "AZUL");
    private static final Set<String> CARTOES = Set.of("AMARELO", "VERMELHO");

    private final EstadoArbitragemRepository estadoRepository;
    private final PartidaRepository partidaRepository;
    private final InscricaoRepository inscricaoRepository;
    private final ObjectMapper objectMapper;

    public ArbitragemService(EstadoArbitragemRepository estadoRepository,
                             PartidaRepository partidaRepository,
                             InscricaoRepository inscricaoRepository,
                             ObjectMapper objectMapper) {
        this.estadoRepository = estadoRepository;
        this.partidaRepository = partidaRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public EstadoArbitragemResponse obter(UUID partidaId) {
        validarPartida(partidaId);
        return estadoRepository.findByPartidaId(partidaId)
                .map(this::responder)
                .orElseGet(() -> new EstadoArbitragemResponse(
                        partidaId,
                        StatusArbitragem.PREPARACAO,
                        0,
                        DadosArbitragemRequest.vazio(),
                        0,
                        null
                ));
    }

    @Transactional(readOnly = true)
    public ResumoArbitragemResponse resumir(UUID partidaId) {
        return estadoRepository.findByPartidaId(partidaId)
                .map(estado -> criarResumo(estado.getStatus(), estado.segundosAtuais(), lerDados(estado.getDadosJson())))
                .orElseGet(() -> criarResumo(StatusArbitragem.PREPARACAO, 0, DadosArbitragemRequest.vazio()));
    }

    @Transactional
    public EstadoArbitragemResponse atualizar(UUID partidaId, AtualizarEstadoArbitragemRequest request) {
        Partida partida = validarPartida(partidaId);
        EstadoArbitragem estado = estadoRepository.findByPartidaId(partidaId).orElse(null);

        if (estado != null && !estado.getVersao().equals(request.versao())) {
            throw new ObjectOptimisticLockingFailureException(EstadoArbitragem.class, partidaId);
        }
        if (estado == null && request.versao() != 0) {
            throw new ObjectOptimisticLockingFailureException(EstadoArbitragem.class, partidaId);
        }

        validarTransicao(estado == null ? StatusArbitragem.PREPARACAO : estado.getStatus(), request.status());
        validarDados(partidaId, request.status(), request.dados());

        String dadosJson = escreverDados(request.dados());
        if (estado == null) {
            estado = new EstadoArbitragem(partida, dadosJson);
        }
        estado.atualizar(request.status(), request.segundos(), dadosJson);
        estadoRepository.saveAndFlush(estado);
        return responder(estado);
    }

    private Partida validarPartida(UUID partidaId) {
        return partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida não encontrada."));
    }

    private void validarDados(UUID partidaId, StatusArbitragem status, DadosArbitragemRequest dados) {
        List<Inscricao> confirmadas = inscricaoRepository.findByPartidaIdAndStatusIn(
                partidaId,
                List.of(StatusInscricao.CONFIRMADA, StatusInscricao.PRESENTE)
        );
        Set<UUID> jogadores = confirmadas.stream()
                .map(inscricao -> inscricao.getJogador().getId())
                .collect(Collectors.toSet());

        if (!jogadores.containsAll(dados.escala().keySet())) {
            throw new RegraNegocioException("A escalação contém jogador que não está confirmado na partida.");
        }
        if (dados.escala().values().stream().anyMatch(time -> !TIMES.contains(time))) {
            throw new RegraNegocioException("A escalação contém um time inválido.");
        }

        int limite = (jogadores.size() + 1) / 2;
        Map<String, Long> porTime = dados.escala().values().stream()
                .collect(Collectors.groupingBy(time -> time, Collectors.counting()));
        if (porTime.getOrDefault("AMARELO", 0L) > limite || porTime.getOrDefault("AZUL", 0L) > limite) {
            throw new RegraNegocioException("Um time não pode receber mais da metade dos jogadores confirmados.");
        }
        if (status != StatusArbitragem.PREPARACAO && dados.escala().size() != jogadores.size()) {
            throw new RegraNegocioException("Todos os jogadores confirmados devem ser escalados antes do início.");
        }

        for (GolArbitragemRequest gol : dados.gols()) {
            validarEvento(gol.jogadorId(), gol.time(), jogadores, dados.escala());
        }
        for (PunicaoArbitragemRequest punicao : dados.punicoes()) {
            validarEvento(punicao.jogadorId(), punicao.time(), jogadores, dados.escala());
            if (!CARTOES.contains(punicao.tipo())) {
                throw new RegraNegocioException("Tipo de cartão inválido.");
            }
        }
    }

    private void validarEvento(UUID jogadorId, String time, Set<UUID> jogadores, Map<UUID, String> escala) {
        if (!jogadores.contains(jogadorId) || !TIMES.contains(time) || !time.equals(escala.get(jogadorId))) {
            throw new RegraNegocioException("O evento informado não corresponde à escalação da partida.");
        }
    }

    private void validarTransicao(StatusArbitragem atual, StatusArbitragem novo) {
        boolean permitida = switch (atual) {
            case PREPARACAO -> novo == StatusArbitragem.PREPARACAO || novo == StatusArbitragem.EM_ANDAMENTO;
            case EM_ANDAMENTO -> novo == StatusArbitragem.EM_ANDAMENTO || novo == StatusArbitragem.PAUSADA || novo == StatusArbitragem.FINALIZADA;
            case PAUSADA -> novo == StatusArbitragem.PAUSADA || novo == StatusArbitragem.EM_ANDAMENTO || novo == StatusArbitragem.FINALIZADA;
            case FINALIZADA -> novo == StatusArbitragem.FINALIZADA;
        };
        if (!permitida) {
            throw new RegraNegocioException("Transição inválida do estado da partida.");
        }
    }

    private String escreverDados(DadosArbitragemRequest dados) {
        try {
            return objectMapper.writeValueAsString(dados);
        } catch (Exception excecao) {
            throw new IllegalStateException("Não foi possível armazenar os dados da arbitragem.", excecao);
        }
    }

    private DadosArbitragemRequest lerDados(String json) {
        try {
            return objectMapper.readValue(json, DadosArbitragemRequest.class);
        } catch (Exception excecao) {
            throw new IllegalStateException("Não foi possível recuperar os dados da arbitragem.", excecao);
        }
    }

    private EstadoArbitragemResponse responder(EstadoArbitragem estado) {
        return new EstadoArbitragemResponse(
                estado.getPartida().getId(),
                estado.getStatus(),
                estado.segundosAtuais(),
                lerDados(estado.getDadosJson()),
                estado.getVersao(),
                estado.getAtualizadoEm()
        );
    }

    private ResumoArbitragemResponse criarResumo(StatusArbitragem status, int segundos,
                                                   DadosArbitragemRequest dados) {
        int golsAmarelo = (int) dados.gols().stream().filter(gol -> "AMARELO".equals(gol.time())).count();
        int golsAzul = (int) dados.gols().stream().filter(gol -> "AZUL".equals(gol.time())).count();
        int cartoesAmarelos = (int) dados.punicoes().stream().filter(p -> "AMARELO".equals(p.tipo())).count();
        int cartoesVermelhos = (int) dados.punicoes().stream().filter(p -> "VERMELHO".equals(p.tipo())).count();

        Map<UUID, Integer> amarelosPorJogador = new HashMap<>();
        Set<UUID> expulsos = dados.punicoes().stream()
                .filter(p -> "VERMELHO".equals(p.tipo()))
                .map(PunicaoArbitragemRequest::jogadorId)
                .collect(Collectors.toSet());
        dados.punicoes().stream()
                .filter(p -> "AMARELO".equals(p.tipo()))
                .forEach(p -> amarelosPorJogador.merge(p.jogadorId(), 1, Integer::sum));
        amarelosPorJogador.forEach((jogadorId, quantidade) -> {
            if (quantidade >= 2) expulsos.add(jogadorId);
        });

        return new ResumoArbitragemResponse(
                status,
                golsAmarelo,
                golsAzul,
                dados.gols().size(),
                dados.punicoes().size(),
                cartoesAmarelos,
                cartoesVermelhos,
                expulsos.size(),
                dados.acrescimos(),
                segundos
        );
    }
}

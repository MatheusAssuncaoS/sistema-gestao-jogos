package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.Inscricao;
import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.Partida;
import br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao;
import br.com.matheusassuncao.gestaojogos.dominio.StatusPartida;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.InscricaoRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.JogadorRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * UC06 e UC07: inscrição e cancelamento.
 *
 * O ponto crítico do sistema. A contagem de vagas e a criação da inscrição
 * precisam ser atômicas: sem isso, dois jogadores clicando ao mesmo tempo na
 * última vaga passariam ambos pela verificação e a partida terminaria com
 * dezessete confirmados.
 *
 * A garantia vem de um lock pessimista na linha da partida
 * (SELECT ... FOR UPDATE). Enquanto uma transação o segura, as demais
 * aguardam alguns milissegundos e depois enxergam a contagem já atualizada.
 * O lock otimista não serviria aqui: em pico de inscrições, choveriam
 * conflitos e seria preciso implementar retry.
 */
@Service
public class InscricaoService {

    private static final Logger log = LoggerFactory.getLogger(InscricaoService.class);

    private final InscricaoRepository inscricaoRepository;
    private final PartidaRepository partidaRepository;
    private final JogadorRepository jogadorRepository;
    private final UsuarioRepository usuarioRepository;

    public InscricaoService(InscricaoRepository inscricaoRepository,
                            PartidaRepository partidaRepository,
                            JogadorRepository jogadorRepository,
                            UsuarioRepository usuarioRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.partidaRepository = partidaRepository;
        this.jogadorRepository = jogadorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * UC06: inscrever-se em partida.
     *
     * A ordem importa: tudo que não depende da partida é validado antes de
     * pegar o lock, para segurá-lo pelo menor tempo possível.
     */
    @Transactional
    public Inscricao inscrever(UUID partidaId, String emailDoJogador) {
        Jogador jogador = buscarJogadorAptoOuFalhar(emailDoJogador);

        // A partir daqui a partida está bloqueada para qualquer outra
        // inscrição ou cancelamento, até o fim desta transação.
        Partida partida = partidaRepository.buscarPorIdComBloqueio(partidaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida não encontrada."));

        validarPartidaAbertaParaInscricao(partida);

        inscricaoRepository.buscarInscricaoAtiva(partidaId, jogador.getId())
                .ifPresent(existente -> {
                    throw new RegraNegocioException(
                            "Você já possui uma inscrição ativa nesta partida."
                    );
                });

        long confirmados = inscricaoRepository.contarConfirmados(partidaId);

        // RN02 e RN04. A lista de espera (RN12) entra no Marco 2: por ora a
        // partida lotada simplesmente recusa novas inscrições.
        if (confirmados >= partida.getCapacidade()) {
            throw new RegraNegocioException(
                    "Esta partida já atingiu o limite de %d jogadores."
                            .formatted(partida.getCapacidade())
            );
        }

        Inscricao inscricao = inscricaoRepository.save(
                new Inscricao(partida, jogador, StatusInscricao.CONFIRMADA)
        );

        if (confirmados + 1 == partida.getCapacidade()) {
            partida.marcarComoLotada();
        }

        log.info("Jogador {} inscrito na partida {} ({}/{}).",
                jogador.getId(), partidaId, confirmados + 1, partida.getCapacidade());

        // A resposta HTTP é montada fora desta transação (open-in-view
        // desligado): inicializa o proxy do local aqui, enquanto a sessão
        // ainda está aberta, para que InscricaoResponse.de() possa lê-lo.
        Hibernate.initialize(partida.getLocal());

        return inscricao;
    }

    /**
     * UC07: cancelar inscrição.
     *
     * Usa o mesmo lock da inscrição, e na mesma ordem (partida primeiro),
     * para que um cancelamento concorrente com uma inscrição não permita que
     * ambas enxerguem a mesma vaga.
     */
    @Transactional
    public void cancelar(UUID partidaId, String emailDoJogador) {
        Usuario usuario = buscarUsuario(emailDoJogador);

        Jogador jogador = jogadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RegraNegocioException(
                        "Seu cadastro ainda não foi aprovado."
                ));

        Partida partida = partidaRepository.buscarPorIdComBloqueio(partidaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida não encontrada."));

        if (partida.getInicio().isBefore(OffsetDateTime.now())) {
            throw new RegraNegocioException(
                    "Não é possível cancelar a inscrição de uma partida que já começou."
            );
        }

        Inscricao inscricao = inscricaoRepository
                .buscarInscricaoAtiva(partidaId, jogador.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Você não possui inscrição ativa nesta partida."
                ));

        inscricao.cancelar(usuario, "Cancelado pelo próprio jogador.");

        // RN15: a vaga liberada reabre a partida.
        // A promoção da lista de espera (RN12) entra aqui no Marco 2.
        partida.reabrirSeLotada();

        log.info("Inscrição {} cancelada na partida {}.", inscricao.getId(), partidaId);
    }

    @Transactional(readOnly = true)
    public List<Inscricao> listarInscritos(UUID partidaId) {
        if (!partidaRepository.existsById(partidaId)) {
            throw new RecursoNaoEncontradoException("Partida não encontrada.");
        }

        return inscricaoRepository.listarAtivasDaPartida(partidaId);
    }

    @Transactional(readOnly = true)
    public List<Inscricao> listarMinhasInscricoes(String emailDoJogador) {
        Usuario usuario = buscarUsuario(emailDoJogador);

        return jogadorRepository.findByUsuarioId(usuario.getId())
                .map(jogador -> inscricaoRepository.listarAtivasDoJogador(jogador.getId()))
                .orElseGet(List::of);
    }

    /**
     * Chamado quando o organizador cancela a partida: as inscrições ativas
     * são canceladas junto, com o motivo registrado.
     */
    @Transactional
    public int cancelarInscricoesDaPartida(Partida partida, Usuario responsavel) {
        List<Inscricao> ativas = inscricaoRepository.listarAtivasDaPartida(partida.getId());

        ativas.forEach(inscricao ->
                inscricao.cancelar(responsavel, "Partida cancelada pelo organizador.")
        );

        if (!ativas.isEmpty()) {
            log.info("{} inscrições canceladas junto com a partida {}.",
                    ativas.size(), partida.getId());
        }

        return ativas.size();
    }

    /**
     * RN01 e RN08: só se inscreve quem está ativo, aprovado, regular e sem
     * suspensão vigente.
     *
     * A checagem de suspensão entra no Marco 2, junto com o controle
     * disciplinar (UC16).
     */
    private Jogador buscarJogadorAptoOuFalhar(String email) {
        Usuario usuario = buscarUsuario(email);

        Jogador jogador = jogadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RegraNegocioException(
                        "Seu cadastro ainda não foi aprovado por um administrador."
                ));

        if (!jogador.estaRegular()) {
            throw new RegraNegocioException(
                    "Sua situação associativa não está regular. Procure a administração do clube."
            );
        }

        return jogador;
    }

    private void validarPartidaAbertaParaInscricao(Partida partida) {
        if (partida.getStatus() == StatusPartida.RASCUNHO) {
            throw new RegraNegocioException("Esta partida ainda não foi aberta para inscrições.");
        }

        if (partida.getStatus().estaEncerrada()) {
            throw new RegraNegocioException("Esta partida não aceita mais inscrições.");
        }

        OffsetDateTime agora = OffsetDateTime.now();

        if (partida.getInscricoesAbremEm() != null
                && agora.isBefore(partida.getInscricoesAbremEm())) {
            throw new RegraNegocioException("As inscrições para esta partida ainda não abriram.");
        }

        if (partida.getInscricoesEncerramEm() != null
                && agora.isAfter(partida.getInscricoesEncerramEm())) {
            throw new RegraNegocioException("O período de inscrição desta partida já encerrou.");
        }

        if (partida.getInicio().isBefore(agora)) {
            throw new RegraNegocioException("Esta partida já começou.");
        }
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }
}

package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.Categoria;
import br.com.matheusassuncao.gestaojogos.dominio.LocalPartida;
import br.com.matheusassuncao.gestaojogos.dominio.Modalidade;
import br.com.matheusassuncao.gestaojogos.dominio.Partida;
import br.com.matheusassuncao.gestaojogos.dominio.StatusPartida;
import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import br.com.matheusassuncao.gestaojogos.dto.CriarPartidaRequest;
import br.com.matheusassuncao.gestaojogos.dto.EditarPartidaRequest;
import br.com.matheusassuncao.gestaojogos.dto.PartidaResponse;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.CategoriaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.LocalPartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.ModalidadeRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.PartidaRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * UC11: gerenciar partidas.
 */
@Service
public class PartidaService {

    private static final Logger log = LoggerFactory.getLogger(PartidaService.class);

    private final PartidaRepository partidaRepository;
    private final ModalidadeRepository modalidadeRepository;
    private final LocalPartidaRepository localPartidaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalendarioService calendarioService;
    private final InscricaoService inscricaoService;

    public PartidaService(PartidaRepository partidaRepository,
                          ModalidadeRepository modalidadeRepository,
                          LocalPartidaRepository localPartidaRepository,
                          CategoriaRepository categoriaRepository,
                          UsuarioRepository usuarioRepository,
                          CalendarioService calendarioService,
                          InscricaoService inscricaoService) {
        this.partidaRepository = partidaRepository;
        this.modalidadeRepository = modalidadeRepository;
        this.localPartidaRepository = localPartidaRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.calendarioService = calendarioService;
        this.inscricaoService = inscricaoService;
    }

    @Transactional(readOnly = true)
    public List<PartidaResponse> listarFuturas() {
        return partidaRepository.findByInicioAfterOrderByInicio(OffsetDateTime.now())
                .stream()
                .map(PartidaResponse::de)
                .toList();
    }

    /**
     * UC05: partidas que o jogador pode ver e nas quais pode se inscrever.
     */
    @Transactional(readOnly = true)
    public List<PartidaResponse> listarAbertas() {
        return partidaRepository
                .findByStatusInAndInicioAfterOrderByInicio(
                        List.of(StatusPartida.ABERTA, StatusPartida.LOTADA),
                        OffsetDateTime.now()
                )
                .stream()
                .map(PartidaResponse::de)
                .toList();
    }

    private Partida buscar(UUID partidaId) {
        return partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida não encontrada."));
    }

    @Transactional(readOnly = true)
    public PartidaResponse detalhar(UUID partidaId) {
        return PartidaResponse.de(buscar(partidaId));
    }

    /**
     * A partida nasce em RASCUNHO, com as duas equipes criadas junto.
     *
     * A data é validada contra o calendário (RN06 e RN07) antes de qualquer
     * escrita: o clube só realiza partidas em dias e horários configurados,
     * e feriados e recessos bloqueiam o agendamento.
     */
    @Transactional
    public PartidaResponse criar(CriarPartidaRequest request, String emailDoOrganizador) {
        Modalidade modalidade = modalidadeRepository.findById(request.modalidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modalidade não encontrada."));

        LocalPartida local = buscarLocal(request.localId());
        Categoria categoria = buscarCategoriaOpcional(request.categoriaId());
        Usuario organizador = buscarOrganizador(emailDoOrganizador);

        calendarioService.validarDataDisponivel(request.inicio());

        validarPeriodoDeInscricao(
                request.inscricoesAbremEm(),
                request.inscricoesEncerramEm(),
                request.inicio()
        );

        Partida partida = new Partida(
                modalidade,
                local,
                categoria,
                request.inicio(),
                request.capacidadeOuPadrao(),
                request.inscricoesAbremEm(),
                request.inscricoesEncerramEm(),
                organizador
        );

        partidaRepository.save(partida);

        log.info("Partida {} criada para {}.", partida.getId(), partida.getInicio());

        return PartidaResponse.de(partida);
    }

    /**
     * Edição com lock otimista: se a versão enviada não for a atual, o
     * Hibernate rejeita o update e a exceção vira 409 no TratadorDeErros.
     */
    @Transactional
    public PartidaResponse editar(UUID partidaId, EditarPartidaRequest request) {
        Partida partida = buscar(partidaId);

        if (!request.versao().equals(partida.getVersao())) {
            throw new ObjectOptimisticLockingFailureException(Partida.class, partidaId);
        }

        LocalPartida local = buscarLocal(request.localId());
        Categoria categoria = buscarCategoriaOpcional(request.categoriaId());

        calendarioService.validarDataDisponivel(request.inicio());

        validarPeriodoDeInscricao(
                request.inscricoesAbremEm(),
                request.inscricoesEncerramEm(),
                request.inicio()
        );

        partida.alterarDados(
                local,
                categoria,
                request.inicio(),
                request.inscricoesAbremEm(),
                request.inscricoesEncerramEm()
        );

        partidaRepository.flush();

        return PartidaResponse.de(partida);
    }

    @Transactional
    public PartidaResponse abrir(UUID partidaId) {
        Partida partida = buscar(partidaId);
        partida.abrir();
        partidaRepository.flush();

        log.info("Partida {} aberta para inscrições.", partidaId);

        return PartidaResponse.de(partida);
    }

    /**
     * Cancelar a partida cancela junto todas as inscrições ativas: quem
     * estava confirmado precisa saber que o jogo não vai acontecer.
     */
    @Transactional
    public PartidaResponse cancelar(UUID partidaId, String emailDoOrganizador) {
        Partida partida = buscar(partidaId);
        partida.cancelar();

        Usuario organizador = usuarioRepository.findByEmailIgnoreCase(emailDoOrganizador)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organizador não encontrado."));

        int canceladas = inscricaoService.cancelarInscricoesDaPartida(partida, organizador);

        log.info("Partida {} cancelada, com {} inscrições afetadas.", partidaId, canceladas);

        return PartidaResponse.de(partida);
    }

    private LocalPartida buscarLocal(UUID localId) {
        return localPartidaRepository.findById(localId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
    }

    private Categoria buscarCategoriaOpcional(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }

        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }

    private Usuario buscarOrganizador(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organizador não encontrado."));
    }

    private void validarPeriodoDeInscricao(OffsetDateTime abrem,
                                           OffsetDateTime encerram,
                                           OffsetDateTime inicioDaPartida) {

        if (abrem != null && encerram != null && !encerram.isAfter(abrem)) {
            throw new RegraNegocioException(
                    "O encerramento das inscrições deve ser posterior à abertura."
            );
        }

        if (encerram != null && encerram.isAfter(inicioDaPartida)) {
            throw new RegraNegocioException(
                    "As inscrições devem encerrar antes do início da partida."
            );
        }
    }
}

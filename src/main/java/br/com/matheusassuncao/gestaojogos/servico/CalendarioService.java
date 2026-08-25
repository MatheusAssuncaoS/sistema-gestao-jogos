package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.*;
import br.com.matheusassuncao.gestaojogos.dto.AgendaRequest;
import br.com.matheusassuncao.gestaojogos.dominio.ExcecaoCalendario;
import br.com.matheusassuncao.gestaojogos.dominio.TipoExcecao;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.DiaFuncionamentoRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.ExcecaoCalendarioRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UC21: gerenciar calendário.
 *
 * Concentra a validação das RN06 e RN07, usada pela criação e edição de
 * partidas.
 */
@Service
public class CalendarioService {

    /**
     * O calendário do clube é local: um jogo de segunda às 19h é 19h no fuso
     * do clube, independentemente do fuso de quem envia a requisição.
     */
    private static final ZoneId FUSO_DO_CLUBE = ZoneId.of("America/Sao_Paulo");

    private final DiaFuncionamentoRepository diaFuncionamentoRepository;
    private final ExcecaoCalendarioRepository excecaoCalendarioRepository;
    private final AgendaDisponibilidadeRepository agendaRepository;
    private final LocalPartidaRepository localRepository;
    private final ModalidadeRepository modalidadeRepository;
    private final CategoriaRepository categoriaRepository;

    public CalendarioService(DiaFuncionamentoRepository diaFuncionamentoRepository,
                             ExcecaoCalendarioRepository excecaoCalendarioRepository,
                             AgendaDisponibilidadeRepository agendaRepository,
                             LocalPartidaRepository localRepository, ModalidadeRepository modalidadeRepository,
                             CategoriaRepository categoriaRepository) {
        this.diaFuncionamentoRepository = diaFuncionamentoRepository;
        this.excecaoCalendarioRepository = excecaoCalendarioRepository;
        this.agendaRepository = agendaRepository;
        this.localRepository = localRepository;
        this.modalidadeRepository = modalidadeRepository;
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * RN06 e RN07: a data precisa cair em um horário configurado e não pode
     * estar dentro de um feriado ou recesso.
     */
    @Transactional(readOnly = true)
    public void validarDataDisponivel(OffsetDateTime inicio) {
        LocalDateTime local = inicio.atZoneSameInstant(FUSO_DO_CLUBE).toLocalDateTime();

        excecaoCalendarioRepository.buscarQueCobre(local.toLocalDate())
                .ifPresent(excecao -> {
                    throw new RegraNegocioException(
                            "Não há partidas neste período: %s (%s)."
                                    .formatted(excecao.getDescricao(),
                                            excecao.getTipo().name().toLowerCase())
                    );
                });

        boolean horarioConfigurado = diaFuncionamentoRepository
                .existsByDiaDaSemanaAndHorarioAndAtivoTrue(
                        local.getDayOfWeek(),
                        local.toLocalTime()
                );

        if (!horarioConfigurado) {
            throw new RegraNegocioException(
                    "O clube não realiza partidas neste dia e horário. "
                            + "Consulte os horários disponíveis."
            );
        }
    }

    @Transactional(readOnly = true)
    public void validarDataDisponivel(OffsetDateTime inicio, UUID localId, UUID modalidadeId, Long categoriaId) {
        LocalDateTime local = inicio.atZoneSameInstant(FUSO_DO_CLUBE).toLocalDateTime();
        excecaoCalendarioRepository.buscarQueCobre(local.toLocalDate()).ifPresent(excecao -> {
            throw new RegraNegocioException("Não há partidas neste período: " + excecao.getDescricao() + ".");
        });
        List<AgendaDisponibilidade> agendas = agendaRepository.findByAtivoTrueOrderByInicioAscNomeAsc();
        if (agendas.isEmpty()) { validarDataDisponivel(inicio); return; }
        boolean disponivel = agendas.stream().anyMatch(agenda -> agenda.contempla(local.toLocalDate(), local.getDayOfWeek(),
                local.toLocalTime(), localId, modalidadeId, categoriaId));
        if (!disponivel) throw new RegraNegocioException("O local, modalidade, categoria e horário não pertencem a uma agenda disponível.");
    }

    @Transactional(readOnly = true)
    public List<AgendaDisponibilidade> listarAgendas() { return agendaRepository.findByAtivoTrueOrderByInicioAscNomeAsc(); }

    @Transactional
    public AgendaDisponibilidade criarAgenda(AgendaRequest request) { return agendaRepository.save(montarAgenda(null, request)); }

    @Transactional
    public AgendaDisponibilidade editarAgenda(UUID id, AgendaRequest request) {
        AgendaDisponibilidade agenda = agendaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agenda não encontrada."));
        montarAgenda(agenda, request); return agenda;
    }

    @Transactional
    public void excluirAgenda(UUID id) {
        AgendaDisponibilidade agenda = agendaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agenda não encontrada."));
        agenda.desativar();
    }

    private AgendaDisponibilidade montarAgenda(AgendaDisponibilidade existente, AgendaRequest request) {
        if (request.fim().isBefore(request.inicio())) throw new RegraNegocioException("A data final deve ser igual ou posterior à inicial.");
        LocalPartida local = localRepository.findById(request.localId()).orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
        Modalidade modalidade = modalidadeRepository.findById(request.modalidadeId()).orElseThrow(() -> new RecursoNaoEncontradoException("Modalidade não encontrada."));
        Categoria categoria = request.categoriaId() == null ? null : categoriaRepository.findById(request.categoriaId()).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
        Map<DayOfWeek, List<LocalTime>> regras = request.regras().stream().collect(Collectors.toMap(AgendaRequest.RegraAgendaRequest::diaDaSemana,
                AgendaRequest.RegraAgendaRequest::horarios, (a,b) -> { var uniao = new ArrayList<>(a); uniao.addAll(b); return uniao; }));
        if (existente == null) return new AgendaDisponibilidade(request.nome(), local, modalidade, categoria, request.inicio(), request.fim(), regras);
        existente.atualizar(request.nome(), local, modalidade, categoria, request.inicio(), request.fim(), regras); return existente;
    }

    /**
     * Próximos horários em que é possível agendar, já descontando feriados e
     * recessos. Útil para o cliente montar a agenda.
     */
    @Transactional(readOnly = true)
    public List<OffsetDateTime> listarProximosHorarios(int diasAFrente) {
        List<AgendaDisponibilidade> agendas = agendaRepository.findByAtivoTrueOrderByInicioAscNomeAsc();
        if (!agendas.isEmpty()) {
            LocalDate hoje = LocalDate.now(FUSO_DO_CLUBE);
            LocalDate limite = hoje.plusDays(diasAFrente);
            OffsetDateTime agora = OffsetDateTime.now();
            List<ExcecaoCalendario> excecoes = excecaoCalendarioRepository.findByFimGreaterThanEqualOrderByInicio(hoje);
            return agendas.stream().flatMap(agenda -> agenda.getHorarios().stream().flatMap(regra -> {
                        LocalDate primeiro = agenda.getInicio().isAfter(hoje) ? agenda.getInicio() : hoje;
                        LocalDate ultimo = agenda.getFim().isBefore(limite) ? agenda.getFim() : limite;
                        if (ultimo.isBefore(primeiro)) return java.util.stream.Stream.empty();
                        return primeiro.datesUntil(ultimo.plusDays(1))
                                .filter(data -> data.getDayOfWeek() == regra.getDiaDaSemana())
                                .filter(data -> excecoes.stream().noneMatch(excecao -> excecao.cobre(data)))
                                .map(data -> data.atTime(regra.getHorario()).atZone(FUSO_DO_CLUBE).toOffsetDateTime())
                                .filter(data -> data.isAfter(agora));
                    })).distinct().sorted().toList();
        }
        List<DiaFuncionamento> configurados =
                diaFuncionamentoRepository.findByAtivoTrueOrderByDiaDaSemanaAscHorarioAsc();

        List<OffsetDateTime> horarios = new ArrayList<>();
        LocalDate hoje = LocalDate.now(FUSO_DO_CLUBE);
        List<ExcecaoCalendario> excecoesFuturas =
                excecaoCalendarioRepository.findByFimGreaterThanEqualOrderByInicio(hoje);

        for (int dia = 0; dia <= diasAFrente; dia++) {
            LocalDate data = hoje.plusDays(dia);

            if (excecoesFuturas.stream().anyMatch(excecao -> excecao.cobre(data))) {
                continue;
            }

            configurados.stream()
                    .filter(configurado -> configurado.getDiaDaSemana() == data.getDayOfWeek())
                    .map(configurado -> data.atTime(configurado.getHorario())
                            .atZone(FUSO_DO_CLUBE)
                            .toOffsetDateTime())
                    .filter(horario -> horario.isAfter(OffsetDateTime.now()))
                    .forEach(horarios::add);
        }

        return horarios.stream().sorted().toList();
    }

    @Transactional(readOnly = true)
    public List<DiaFuncionamento> listarDiasDeFuncionamento() {
        return diaFuncionamentoRepository.findByAtivoTrueOrderByDiaDaSemanaAscHorarioAsc();
    }

    @Transactional
    public DiaFuncionamento adicionarDiaDeFuncionamento(DayOfWeek diaDaSemana, LocalTime horario) {
        return diaFuncionamentoRepository
                .findByDiaDaSemanaAndHorario(diaDaSemana, horario)
                .map(existente -> {
                    existente.ativar();
                    return existente;
                })
                .orElseGet(() -> diaFuncionamentoRepository.save(
                        new DiaFuncionamento(diaDaSemana, horario)
                ));
    }

    /**
     * Desativa em vez de apagar: partidas já criadas naquele horário
     * continuam válidas e o histórico da configuração é preservado.
     */
    @Transactional
    public void removerDiaDeFuncionamento(UUID diaId) {
        DiaFuncionamento dia = diaFuncionamentoRepository.findById(diaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Dia de funcionamento não encontrado."
                ));

        dia.desativar();
    }

    @Transactional
    public DiaFuncionamento editarDiaDeFuncionamento(UUID diaId, DayOfWeek diaDaSemana, LocalTime horario) {
        DiaFuncionamento dia = diaFuncionamentoRepository.findById(diaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dia de funcionamento não encontrado."));
        diaFuncionamentoRepository.findByDiaDaSemanaAndHorario(diaDaSemana, horario)
                .filter(existente -> !existente.getId().equals(diaId))
                .ifPresent(item -> { throw new RegraNegocioException("Esse dia e horário já estão cadastrados."); });
        dia.atualizar(diaDaSemana, horario);
        return dia;
    }

    @Transactional(readOnly = true)
    public List<ExcecaoCalendario> listarExcecoesVigentes() {
        return excecaoCalendarioRepository
                .findByFimGreaterThanEqualOrderByInicio(LocalDate.now(FUSO_DO_CLUBE));
    }

    @Transactional
    public ExcecaoCalendario adicionarExcecao(String descricao,
                                              TipoExcecao tipo,
                                              LocalDate inicio,
                                              LocalDate fim) {

        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException(
                    "A data final deve ser igual ou posterior à inicial."
            );
        }

        return excecaoCalendarioRepository.save(
                new ExcecaoCalendario(descricao.trim(), tipo, inicio, fim)
        );
    }

    @Transactional
    public void removerExcecao(UUID excecaoId) {
        ExcecaoCalendario excecao = excecaoCalendarioRepository.findById(excecaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Exceção de calendário não encontrada."
                ));

        excecaoCalendarioRepository.delete(excecao);
    }

    @Transactional
    public ExcecaoCalendario editarExcecao(UUID excecaoId, String descricao, TipoExcecao tipo,
                                           LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException("A data final deve ser igual ou posterior à inicial.");
        }
        ExcecaoCalendario excecao = excecaoCalendarioRepository.findById(excecaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exceção de calendário não encontrada."));
        excecao.atualizar(descricao, tipo, inicio, fim);
        return excecao;
    }
}

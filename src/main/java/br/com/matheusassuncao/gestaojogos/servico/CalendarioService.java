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

    public CalendarioService(DiaFuncionamentoRepository diaFuncionamentoRepository,
                             ExcecaoCalendarioRepository excecaoCalendarioRepository,
                             AgendaDisponibilidadeRepository agendaRepository,
                             LocalPartidaRepository localRepository) {
        this.diaFuncionamentoRepository = diaFuncionamentoRepository;
        this.excecaoCalendarioRepository = excecaoCalendarioRepository;
        this.agendaRepository = agendaRepository;
        this.localRepository = localRepository;
    }

    /**
     * Dias e horários são definidos na partida. O calendário impede somente
     * datas cobertas por exceções gerais ou do local escolhido.
     */
    @Transactional(readOnly = true)
    public void validarDataDisponivel(OffsetDateTime inicio) {
        validarDataDisponivel(inicio, null);
    }

    @Transactional(readOnly = true)
    public void validarDataDisponivel(OffsetDateTime inicio, UUID localId) {
        LocalDateTime local = inicio.atZoneSameInstant(FUSO_DO_CLUBE).toLocalDateTime();
        excecaoCalendarioRepository.findByAtivoTrueAndFimGreaterThanEqualOrderByInicio(local.toLocalDate())
                .stream().filter(excecao -> excecao.cobre(local.toLocalDate(), localId)).findFirst()
                .ifPresent(excecao -> {
                    throw new RegraNegocioException("Não há partidas neste período: " + excecao.getDescricao() + ".");
                });

    }

    @Transactional(readOnly = true)
    public void validarDataDisponivel(OffsetDateTime inicio, UUID localId, UUID modalidadeId, Long categoriaId) {
        validarDataDisponivel(inicio, localId);
    }

    @Transactional(readOnly = true)
    public List<AgendaDisponibilidade> listarAgendas() { return agendaRepository.findAllByOrderByInicioDescNomeAsc(); }

    @Transactional
    public AgendaDisponibilidade criarAgenda(AgendaRequest request) {
        throw new RegraNegocioException("Use os dias de funcionamento e as exceções do calendário geral.");
    }

    @Transactional
    public AgendaDisponibilidade editarAgenda(UUID id, AgendaRequest request) {
        throw new RegraNegocioException("Use os dias de funcionamento e as exceções do calendário geral.");
    }

    @Transactional
    public void excluirAgenda(UUID id) {
        AgendaDisponibilidade agenda = agendaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agenda não encontrada."));
        agenda.desativar();
    }

    /**
     * Próximos horários em que é possível agendar, já descontando feriados e
     * recessos. Útil para o cliente montar a agenda.
     */
    @Transactional(readOnly = true)
    public List<OffsetDateTime> listarProximosHorarios(int diasAFrente) {
        return listarProximosHorarios(diasAFrente, null);
    }

    @Transactional(readOnly = true)
    public List<OffsetDateTime> listarProximosHorarios(int diasAFrente, UUID localId) {
        List<DiaFuncionamento> configurados =
                diaFuncionamentoRepository.findByAtivoTrueOrderByDiaDaSemanaAscHorarioAsc();

        List<OffsetDateTime> horarios = new ArrayList<>();
        LocalDate hoje = LocalDate.now(FUSO_DO_CLUBE);
        List<ExcecaoCalendario> excecoesFuturas =
                excecaoCalendarioRepository.findByAtivoTrueAndFimGreaterThanEqualOrderByInicio(hoje);

        for (int dia = 0; dia <= diasAFrente; dia++) {
            LocalDate data = hoje.plusDays(dia);

            if (excecoesFuturas.stream().anyMatch(excecao -> excecao.cobre(data, localId))) {
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
                .findByAtivoTrueAndFimGreaterThanEqualOrderByInicio(LocalDate.now(FUSO_DO_CLUBE));
    }

    @Transactional(readOnly = true)
    public List<ExcecaoCalendario> listarExcecoes() {
        return excecaoCalendarioRepository.findAllByOrderByInicioDescDescricaoAsc();
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
    public ExcecaoCalendario adicionarExcecao(String descricao, TipoExcecao tipo, LocalDate inicio,
                                               LocalDate fim, UUID localId) {
        LocalPartida local = resolverLocalExcecao(localId);
        ExcecaoCalendario excecao = adicionarExcecao(descricao, tipo, inicio, fim);
        excecao.definirLocal(local);
        return excecao;
    }

    @Transactional
    public ExcecaoCalendario editarExcecao(UUID id, String descricao, TipoExcecao tipo,
                                            LocalDate inicio, LocalDate fim, UUID localId) {
        LocalPartida local = resolverLocalExcecao(localId);
        ExcecaoCalendario excecao = editarExcecao(id, descricao, tipo, inicio, fim);
        excecao.definirLocal(local);
        return excecao;
    }

    private LocalPartida resolverLocalExcecao(UUID localId) {
        return localId == null ? null : localRepository.findById(localId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Local não encontrado."));
    }

    @Transactional
    public void removerExcecao(UUID excecaoId) {
        ExcecaoCalendario excecao = excecaoCalendarioRepository.findById(excecaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Exceção de calendário não encontrada."
                ));

        excecao.desativar();
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

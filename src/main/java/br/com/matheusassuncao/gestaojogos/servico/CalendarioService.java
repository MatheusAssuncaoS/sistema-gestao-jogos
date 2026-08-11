package br.com.matheusassuncao.gestaojogos.servico;

import br.com.matheusassuncao.gestaojogos.dominio.DiaFuncionamento;
import br.com.matheusassuncao.gestaojogos.dominio.ExcecaoCalendario;
import br.com.matheusassuncao.gestaojogos.dominio.TipoExcecao;
import br.com.matheusassuncao.gestaojogos.excecao.RecursoNaoEncontradoException;
import br.com.matheusassuncao.gestaojogos.excecao.RegraNegocioException;
import br.com.matheusassuncao.gestaojogos.repositorio.DiaFuncionamentoRepository;
import br.com.matheusassuncao.gestaojogos.repositorio.ExcecaoCalendarioRepository;
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

    public CalendarioService(DiaFuncionamentoRepository diaFuncionamentoRepository,
                             ExcecaoCalendarioRepository excecaoCalendarioRepository) {
        this.diaFuncionamentoRepository = diaFuncionamentoRepository;
        this.excecaoCalendarioRepository = excecaoCalendarioRepository;
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

    /**
     * Próximos horários em que é possível agendar, já descontando feriados e
     * recessos. Útil para o cliente montar a agenda.
     */
    @Transactional(readOnly = true)
    public List<OffsetDateTime> listarProximosHorarios(int diasAFrente) {
        List<DiaFuncionamento> configurados =
                diaFuncionamentoRepository.findByAtivoTrueOrderByDiaDaSemanaAscHorarioAsc();

        List<OffsetDateTime> horarios = new ArrayList<>();
        LocalDate hoje = LocalDate.now(FUSO_DO_CLUBE);

        for (int dia = 0; dia <= diasAFrente; dia++) {
            LocalDate data = hoje.plusDays(dia);

            if (excecaoCalendarioRepository.buscarQueCobre(data).isPresent()) {
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
}

package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.*;
import java.time.*;
import java.util.*;

public record AgendaResponse(UUID id, String nome, UUID localId, String local, UUID modalidadeId,
                             String modalidade, Long categoriaId, String categoria, LocalDate inicio,
                             LocalDate fim, Boolean ativo, List<RegraAgendaResponse> regras) {
    public record RegraAgendaResponse(DayOfWeek diaDaSemana, List<LocalTime> horarios) {}
    public static AgendaResponse de(AgendaDisponibilidade agenda) {
        var regras = agenda.getHorarios().stream().collect(java.util.stream.Collectors.groupingBy(
                AgendaHorario::getDiaDaSemana, TreeMap::new,
                java.util.stream.Collectors.mapping(AgendaHorario::getHorario, java.util.stream.Collectors.toList())))
                .entrySet().stream().map(e -> new RegraAgendaResponse(e.getKey(), e.getValue().stream().sorted().toList())).toList();
        return new AgendaResponse(agenda.getId(), agenda.getNome(), agenda.getLocal().getId(), agenda.getLocal().getNome(),
                agenda.getModalidade().getId(), agenda.getModalidade().getNome(), agenda.getCategoria()==null?null:agenda.getCategoria().getId(),
                agenda.getCategoria()==null?null:agenda.getCategoria().getNome(), agenda.getInicio(), agenda.getFim(), agenda.getAtivo(), regras);
    }
}

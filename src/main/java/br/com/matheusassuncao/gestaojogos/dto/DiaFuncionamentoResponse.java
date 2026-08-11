package br.com.matheusassuncao.gestaojogos.dto;

import br.com.matheusassuncao.gestaojogos.dominio.DiaFuncionamento;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record DiaFuncionamentoResponse(
        UUID id,
        DayOfWeek diaDaSemana,
        LocalTime horario
) {

    public static DiaFuncionamentoResponse de(DiaFuncionamento dia) {
        return new DiaFuncionamentoResponse(
                dia.getId(),
                dia.getDiaDaSemana(),
                dia.getHorario()
        );
    }
}

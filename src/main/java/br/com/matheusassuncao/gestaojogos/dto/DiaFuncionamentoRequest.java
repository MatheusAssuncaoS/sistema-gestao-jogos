package br.com.matheusassuncao.gestaojogos.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DiaFuncionamentoRequest(

        @NotNull(message = "O dia da semana é obrigatório.")
        DayOfWeek diaDaSemana,

        @NotNull(message = "O horário é obrigatório.")
        LocalTime horario
) {
}

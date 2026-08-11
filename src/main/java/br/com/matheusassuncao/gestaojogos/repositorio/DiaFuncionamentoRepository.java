package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.DiaFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaFuncionamentoRepository extends JpaRepository<DiaFuncionamento, UUID> {

    List<DiaFuncionamento> findByAtivoTrueOrderByDiaDaSemanaAscHorarioAsc();

    Optional<DiaFuncionamento> findByDiaDaSemanaAndHorario(DayOfWeek diaDaSemana, LocalTime horario);

    boolean existsByDiaDaSemanaAndHorarioAndAtivoTrue(DayOfWeek diaDaSemana, LocalTime horario);
}

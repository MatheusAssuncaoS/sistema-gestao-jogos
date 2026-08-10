package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.Partida;
import br.com.matheusassuncao.gestaojogos.dominio.StatusPartida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {

    List<Partida> findByStatusInOrderByInicio(Collection<StatusPartida> status);

    List<Partida> findByInicioAfterOrderByInicio(OffsetDateTime referencia);
}

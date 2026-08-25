package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.AgendaDisponibilidade;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface AgendaDisponibilidadeRepository extends JpaRepository<AgendaDisponibilidade, UUID> {
    @EntityGraph(attributePaths = {"local", "modalidade", "categoria", "horarios"})
    List<AgendaDisponibilidade> findByAtivoTrueOrderByInicioAscNomeAsc();
}

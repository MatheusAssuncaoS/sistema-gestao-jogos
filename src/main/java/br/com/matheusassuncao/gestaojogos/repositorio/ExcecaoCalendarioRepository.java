package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.ExcecaoCalendario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExcecaoCalendarioRepository extends JpaRepository<ExcecaoCalendario, UUID> {

    /**
     * Exceção que cobre a data informada. Como os períodos não deveriam se
     * sobrepor, basta a primeira encontrada para bloquear a data.
     */
    @Query("""
            SELECT e FROM ExcecaoCalendario e
            WHERE :data BETWEEN e.inicio AND e.fim
            """)
    Optional<ExcecaoCalendario> buscarQueCobre(@Param("data") LocalDate data);

    List<ExcecaoCalendario> findByFimGreaterThanEqualOrderByInicio(LocalDate referencia);
}

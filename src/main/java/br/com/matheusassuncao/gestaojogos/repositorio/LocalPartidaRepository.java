package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.LocalPartida;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocalPartidaRepository extends JpaRepository<LocalPartida, UUID> {

    List<LocalPartida> findByAtivoTrue();

    boolean existsByModalidades_Id(UUID modalidadeId);

    Optional<LocalPartida> findByNomeIgnoreCase(String nome);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LocalPartida l WHERE l.id = :id")
    Optional<LocalPartida> buscarPorIdComBloqueio(@Param("id") UUID id);
}

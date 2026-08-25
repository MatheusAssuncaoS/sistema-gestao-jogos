package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.EstadoArbitragem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EstadoArbitragemRepository extends JpaRepository<EstadoArbitragem, UUID> {
    Optional<EstadoArbitragem> findByPartidaId(UUID partidaId);
}

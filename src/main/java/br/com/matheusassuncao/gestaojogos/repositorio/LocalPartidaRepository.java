package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.LocalPartida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocalPartidaRepository extends JpaRepository<LocalPartida, UUID> {

    List<LocalPartida> findByAtivoTrue();

    Optional<LocalPartida> findByNomeIgnoreCase(String nome);
}

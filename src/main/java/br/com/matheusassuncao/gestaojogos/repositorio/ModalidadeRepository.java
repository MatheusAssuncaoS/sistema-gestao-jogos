package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.Modalidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModalidadeRepository extends JpaRepository<Modalidade, UUID> {

    Optional<Modalidade> findByNomeIgnoreCase(String nome);

    List<Modalidade> findByAtivoTrue();
}

package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.ExcecaoCalendario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExcecaoCalendarioRepository extends JpaRepository<ExcecaoCalendario, UUID> {

    List<ExcecaoCalendario> findByAtivoTrueAndFimGreaterThanEqualOrderByInicio(LocalDate referencia);

    List<ExcecaoCalendario> findAllByOrderByInicioDescDescricaoAsc();
}

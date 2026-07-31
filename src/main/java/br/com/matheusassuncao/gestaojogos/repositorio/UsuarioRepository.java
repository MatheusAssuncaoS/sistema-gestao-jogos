package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Usuários que se cadastraram mas ainda não têm perfil de jogador,
     * ou seja, aguardam aprovação de um administrador (UC27).
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.status = :status
              AND NOT EXISTS (
                  SELECT 1 FROM Jogador j WHERE j.usuario = u
              )
            ORDER BY u.criadoEm
            """)
    List<Usuario> findByStatusAndSemPerfilDeJogador(@Param("status") StatusUsuario status);
}

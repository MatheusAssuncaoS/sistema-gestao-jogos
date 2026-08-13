package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.Jogador;
import br.com.matheusassuncao.gestaojogos.dominio.StatusUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JogadorRepository extends JpaRepository<Jogador, UUID> {

    Optional<Jogador> findByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioId(UUID usuarioId);

    /**
     * Jogadores cujo usuário está no status informado, com busca opcional
     * por trecho do nome ou do e-mail (busca vazia devolve todos).
     */
    @Query("""
            SELECT j FROM Jogador j
            JOIN FETCH j.usuario u
            LEFT JOIN FETCH j.categoria
            WHERE u.status = :status
              AND (:busca = '' OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :busca, '%')))
            ORDER BY u.nome
            """)
    List<Jogador> buscarPorStatusUsuarioEBusca(@Param("status") StatusUsuario status, @Param("busca") String busca);
}

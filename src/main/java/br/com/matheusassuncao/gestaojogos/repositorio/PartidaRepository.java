package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.Partida;
import br.com.matheusassuncao.gestaojogos.dominio.StatusPartida;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {

    /** Atualização condicional atômica: não reabre canceladas e participa do lock otimista. */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Partida p SET p.status = br.com.matheusassuncao.gestaojogos.dominio.StatusPartida.ABERTA,
                p.versao = p.versao + 1
            WHERE p.status = br.com.matheusassuncao.gestaojogos.dominio.StatusPartida.RASCUNHO
                AND p.inscricoesAbremEm IS NOT NULL AND p.inscricoesAbremEm <= :agora
                AND p.inicio > :agora
                AND (p.inscricoesEncerramEm IS NULL OR p.inscricoesEncerramEm > :agora)
            """)
    int abrirInscricoesAgendadas(@Param("agora") OffsetDateTime agora);

    @Query(value = """
            SELECT EXISTS(SELECT 1 FROM partida p WHERE p.local_id = :localId
              AND p.status NOT IN ('CANCELADA', 'EXCLUIDA') AND p.id <> :ignorada
              AND p.inicio < :fim AND p.inicio + p.duracao_minutos * INTERVAL '1 minute' > :inicio)
            """, nativeQuery = true)
    boolean existeSobreposicao(@Param("localId") UUID localId, @Param("inicio") OffsetDateTime inicio,
                               @Param("fim") OffsetDateTime fim, @Param("ignorada") UUID ignorada);

    boolean existsByModalidade_Id(UUID modalidadeId);

    boolean existsByLocal_Id(UUID localId);

    boolean existsByCategoria_Id(Long categoriaId);

    boolean existsByLocal_IdAndInicioAndStatusNot(UUID localId, OffsetDateTime inicio, StatusPartida status);

    boolean existsByLocal_IdAndInicioAndStatusNotAndIdNot(UUID localId, OffsetDateTime inicio, StatusPartida status, UUID id);

    List<Partida> findByStatusInOrderByInicio(Collection<StatusPartida> status);

    List<Partida> findByInicioAfterOrderByInicio(OffsetDateTime referencia);

    List<Partida> findByStatusInAndInicioAfterOrderByInicio(
            Collection<StatusPartida> status,
            OffsetDateTime referencia
    );

    /**
     * SELECT ... FOR UPDATE na linha da partida.
     *
     * É o coração da garantia de capacidade: enquanto uma transação segura
     * este lock, qualquer outra que tente inscrever alguém na mesma partida
     * fica esperando, em vez de contar as vagas em paralelo e confirmar o
     * décimo sétimo jogador.
     *
     * O mesmo lock é usado no cancelamento, e sempre na mesma ordem (partida
     * primeiro), para não abrir espaço para deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Partida p WHERE p.id = :id")
    Optional<Partida> buscarPorIdComBloqueio(@Param("id") UUID id);
}

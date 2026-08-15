package br.com.matheusassuncao.gestaojogos.repositorio;

import br.com.matheusassuncao.gestaojogos.dominio.Inscricao;
import br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InscricaoRepository extends JpaRepository<Inscricao, UUID> {

    boolean existsByJogadorId(UUID jogadorId);

    /**
     * Contagem que decide se ainda há vaga (RN02). Executada sempre dentro do
     * lock pessimista da partida.
     */
    @Query("""
            SELECT COUNT(i) FROM Inscricao i
            WHERE i.partida.id = :partidaId
              AND i.status IN (
                  br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao.CONFIRMADA,
                  br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao.PRESENTE,
                  br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao.AUSENTE
              )
            """)
    long contarConfirmados(@Param("partidaId") UUID partidaId);

    @Query("""
            SELECT i FROM Inscricao i
            WHERE i.partida.id = :partidaId
              AND i.jogador.id = :jogadorId
              AND i.status <> br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao.CANCELADA
            """)
    Optional<Inscricao> buscarInscricaoAtiva(@Param("partidaId") UUID partidaId,
                                             @Param("jogadorId") UUID jogadorId);

    @Query("""
            SELECT i FROM Inscricao i
            JOIN FETCH i.jogador j
            JOIN FETCH j.usuario
            LEFT JOIN FETCH j.categoria
            WHERE i.partida.id = :partidaId
              AND i.status <> br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao.CANCELADA
            ORDER BY i.dataSolicitacao
            """)
    List<Inscricao> listarAtivasDaPartida(@Param("partidaId") UUID partidaId);

    @Query("""
            SELECT i FROM Inscricao i
            JOIN FETCH i.partida p
            JOIN FETCH p.local
            WHERE i.jogador.id = :jogadorId
              AND i.status <> br.com.matheusassuncao.gestaojogos.dominio.StatusInscricao.CANCELADA
            ORDER BY i.dataSolicitacao DESC
            """)
    List<Inscricao> listarAtivasDoJogador(@Param("jogadorId") UUID jogadorId);

    List<Inscricao> findByPartidaIdAndStatusIn(UUID partidaId, Collection<StatusInscricao> status);
}

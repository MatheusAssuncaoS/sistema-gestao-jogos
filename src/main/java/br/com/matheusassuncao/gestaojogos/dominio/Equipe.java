package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Equipe de uma partida. No futebol são duas, Azul e Amarela, com 8 vagas
 * cada. Criadas junto com a partida: uma partida sem equipes seria um
 * estado inválido.
 */
@Entity
@Table(name = "equipe")
public class Equipe {

    public static final String COR_AZUL = "AZUL";
    public static final String COR_AMARELO = "AMARELO";

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @Column(nullable = false, length = 50)
    private String nome;

    @Column(nullable = false, length = 20)
    private String cor;

    @Column(nullable = false)
    private Integer capacidade;

    protected Equipe() {
        // exigido pelo JPA
    }

    public Equipe(Partida partida, String nome, String cor, Integer capacidade) {
        this.partida = partida;
        this.nome = nome;
        this.cor = cor;
        this.capacidade = capacidade;
    }

    public UUID getId() {
        return id;
    }

    public Partida getPartida() {
        return partida;
    }

    public String getNome() {
        return nome;
    }

    public String getCor() {
        return cor;
    }

    public Integer getCapacidade() {
        return capacidade;
    }
}

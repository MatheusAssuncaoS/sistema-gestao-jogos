package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * RN11: novas modalidades entram como registro, sem alteração estrutural.
 */
@Entity
@Table(name = "modalidade")
public class Modalidade {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String nome;

    @Column(nullable = false)
    private Boolean ativo = true;

    protected Modalidade() {
        // exigido pelo JPA
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Boolean getAtivo() {
        return ativo;
    }
}

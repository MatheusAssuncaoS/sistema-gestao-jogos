package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "papel")
public class Papel {

    @Id
    private Short id;

    @Column(nullable = false, length = 30, unique = true)
    private String nome;

    protected Papel() {
    }

    public Short getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
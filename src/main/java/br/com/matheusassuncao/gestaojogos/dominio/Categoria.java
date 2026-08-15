package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String nome;

    /**
     * Peso técnico da categoria, usado no balanceamento das equipes (RN10).
     */
    @Column(nullable = false)
    private Integer peso;

    @Column(nullable = false)
    private Boolean ativo = true;

    protected Categoria() {
        // exigido pelo JPA
    }

    public Categoria(String nome, Integer peso) {
        this.nome = nome.trim();
        this.peso = peso;
    }

    public void atualizar(String nome, Integer peso) {
        this.nome = nome.trim();
        this.peso = peso;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Integer getPeso() {
        return peso;
    }

    public Boolean getAtivo() {
        return ativo;
    }
}

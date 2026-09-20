package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.*;
import java.util.Set;
import java.util.HashSet;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "local_partida")
public class LocalPartida {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "local_modalidade", joinColumns = @JoinColumn(name = "local_id"),
            inverseJoinColumns = @JoinColumn(name = "modalidade_id"))
    private Set<Modalidade> modalidades = new HashSet<>();

    public Set<Modalidade> getModalidades() { return modalidades; }
    public void definirModalidades(Set<Modalidade> modalidades) { this.modalidades = new HashSet<>(modalidades); }
    public void definirAtivo(boolean ativo) { this.ativo = ativo; }
    public boolean permiteModalidade(UUID id) {
        return modalidades.stream().anyMatch(m -> m.getId().equals(id));
    }

    protected LocalPartida() {
        // exigido pelo JPA
    }

    public LocalPartida(String nome, String descricao) {
        this.nome = nome.trim();
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
    }

    public void atualizar(String nome, String descricao) {
        this.nome = nome.trim();
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public Boolean getAtivo() {
        return ativo;
    }
}

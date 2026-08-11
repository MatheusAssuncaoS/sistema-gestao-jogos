package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * RN07: período em que não há partidas.
 *
 * Um feriado tem início e fim na mesma data; um recesso cobre um intervalo.
 * A mesma estrutura atende os dois casos, evitando cadastrar dezessete
 * registros para um recesso de fim de ano.
 */
@Entity
@Table(name = "excecao_calendario")
public class ExcecaoCalendario {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoExcecao tipo;

    @Column(nullable = false)
    private LocalDate inicio;

    @Column(nullable = false)
    private LocalDate fim;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected ExcecaoCalendario() {
        // exigido pelo JPA
    }

    public ExcecaoCalendario(String descricao, TipoExcecao tipo, LocalDate inicio, LocalDate fim) {
        this.descricao = descricao;
        this.tipo = tipo;
        this.inicio = inicio;
        this.fim = fim;
    }

    public boolean cobre(LocalDate data) {
        return !data.isBefore(inicio) && !data.isAfter(fim);
    }

    public UUID getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoExcecao getTipo() {
        return tipo;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}

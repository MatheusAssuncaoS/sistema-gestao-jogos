package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "agenda_disponibilidade")
public class AgendaDisponibilidade {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, length = 120) private String nome;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "local_id") private LocalPartida local;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "modalidade_id") private Modalidade modalidade;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "categoria_id") private Categoria categoria;
    @Column(nullable = false) private LocalDate inicio;
    @Column(nullable = false) private LocalDate fim;
    @Column(nullable = false) private Boolean ativo = true;
    @Column(name = "criado_em", nullable = false) private OffsetDateTime criadoEm = OffsetDateTime.now();
    @OneToMany(mappedBy = "agenda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgendaHorario> horarios = new ArrayList<>();

    protected AgendaDisponibilidade() {}
    public AgendaDisponibilidade(String nome, LocalPartida local, Modalidade modalidade, Categoria categoria,
                                 LocalDate inicio, LocalDate fim, Map<DayOfWeek, List<LocalTime>> regras) {
        atualizar(nome, local, modalidade, categoria, inicio, fim, regras);
    }
    public void atualizar(String nome, LocalPartida local, Modalidade modalidade, Categoria categoria,
                          LocalDate inicio, LocalDate fim, Map<DayOfWeek, List<LocalTime>> regras) {
        this.nome = nome.trim(); this.local = local; this.modalidade = modalidade; this.categoria = categoria;
        this.inicio = inicio; this.fim = fim; this.horarios.clear();
        regras.forEach((dia, horas) -> horas.stream().distinct().forEach(hora -> horarios.add(new AgendaHorario(this, dia, hora))));
    }
    public void desativar() { ativo = false; }
    public boolean contempla(LocalDate data, DayOfWeek dia, LocalTime horario, UUID localId, UUID modalidadeId, Long categoriaId) {
        boolean categoriaCompativel = categoria == null || Objects.equals(categoria.getId(), categoriaId);
        return ativo && !data.isBefore(inicio) && !data.isAfter(fim) && local.getId().equals(localId)
                && modalidade.getId().equals(modalidadeId) && categoriaCompativel
                && horarios.stream().anyMatch(item -> item.getDiaDaSemana() == dia && item.getHorario().equals(horario));
    }
    public UUID getId(){return id;} public String getNome(){return nome;} public LocalPartida getLocal(){return local;}
    public Modalidade getModalidade(){return modalidade;} public Categoria getCategoria(){return categoria;}
    public LocalDate getInicio(){return inicio;} public LocalDate getFim(){return fim;} public Boolean getAtivo(){return ativo;}
    public List<AgendaHorario> getHorarios(){return horarios;}
}

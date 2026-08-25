package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity @Table(name = "agenda_horario")
public class AgendaHorario {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "agenda_id") private AgendaDisponibilidade agenda;
    @Enumerated(EnumType.STRING) @Column(name = "dia_da_semana", nullable = false, length = 20) private DayOfWeek diaDaSemana;
    @Column(nullable = false) private LocalTime horario;
    protected AgendaHorario() {}
    public AgendaHorario(AgendaDisponibilidade agenda, DayOfWeek diaDaSemana, LocalTime horario) { this.agenda=agenda; this.diaDaSemana=diaDaSemana; this.horario=horario; }
    public UUID getId(){return id;} public DayOfWeek getDiaDaSemana(){return diaDaSemana;} public LocalTime getHorario(){return horario;}
}

package br.com.matheusassuncao.gestaojogos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * RN06: dia da semana e horário em que o clube realiza partidas.
 *
 * A regra é recorrente (toda segunda às 19h), e as exceções pontuais ficam
 * na ExcecaoCalendario.
 */
@Entity
@Table(name = "dia_funcionamento")
public class DiaFuncionamento {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_da_semana", nullable = false, length = 20)
    private DayOfWeek diaDaSemana;

    @Column(nullable = false)
    private LocalTime horario;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    protected DiaFuncionamento() {
        // exigido pelo JPA
    }

    public DiaFuncionamento(DayOfWeek diaDaSemana, LocalTime horario) {
        this.diaDaSemana = diaDaSemana;
        this.horario = horario;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public UUID getId() {
        return id;
    }

    public DayOfWeek getDiaDaSemana() {
        return diaDaSemana;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}

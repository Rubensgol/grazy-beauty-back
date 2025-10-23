package com.example.grazy_back.model;

import java.time.Instant;
import java.time.LocalDateTime;

import com.example.grazy_back.enums.StatusAgendamentoEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Agendamento
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Cliente usuario;

    // Data/hora prevista do agendamento (timezone local do sistema; considere armazenar em UTC conforme necessidade)
    private LocalDateTime dataHora;

    private String obs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(20)")
    private StatusAgendamentoEnum status = StatusAgendamentoEnum.PENDENTE;

    private Instant createdAt = Instant.now();
    private Instant finalizedAt;
    private Instant canceledAt;
    private String cancelReason;
    private boolean notificado = false;
    private Instant notificadoEm;
}

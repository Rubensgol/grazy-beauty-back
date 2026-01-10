package com.example.grazy_back.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.example.grazy_back.enums.PlanoEnum;
import com.example.grazy_back.enums.StatusTenantEnum;


@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeNegocio;

    @Column(nullable = false, unique = true)
    private String subdominio;

    @Column(unique = true)
    private String dominioCustomizado;

    @Column(nullable = false)
    private String emailAdmin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlanoEnum plano = PlanoEnum.BASICO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusTenantEnum status = StatusTenantEnum.ATIVO;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean onboardingCompleto = false;

    @Builder.Default
    private Integer agendamentosNoMes = 0;

    @Builder.Default
    private Integer limiteAgendamentosMes = 50;

    @Builder.Default
    private Instant criadoEm = Instant.now();

    private Instant atualizadoEm;

    private Instant suspensaoEm;

    private String motivoSuspensao;

    private String corPrimaria;
    private String corSecundaria;
    private String logoUrl;

    private String databaseSchema;
}

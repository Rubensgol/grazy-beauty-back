package com.example.grazy_back.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Configurações visuais e de negócio de um tenant.
 * Aplicadas no frontend para personalização white-label.
 */
@Entity
@Table(name = "configuracao_tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoTenant 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    // Cores da marca
    private String corPrimaria;
    private String corSecundaria;
    private String corFundo;
    private String corTexto;

    // Branding (LONGTEXT para suportar base64)
    @Column(columnDefinition = "LONGTEXT")
    private String logoUrl;
    
    @Column(columnDefinition = "LONGTEXT")
    private String faviconUrl;
    
    private String nomeExibicao;
    private String slogan;

    // Contato
    private String telefone;
    private String whatsapp;
    private String email;
    private String endereco;

    // Redes sociais
    private String instagram;
    private String facebook;
    private String tiktok;

    // Horário de funcionamento (JSON)
    @Column(columnDefinition = "TEXT")
    private String horarioFuncionamento;

    // Configurações de agendamento
    @Builder.Default
    private Integer intervaloAgendamentoMinutos = 30;

    @Builder.Default
    private Integer antecedenciaMinimaHoras = 2;

    @Builder.Default
    private Integer antecedenciaMaximaDias = 30;

    // Notificações
    @Builder.Default
    private boolean notificacoesEmailAtivas = true;

    @Builder.Default
    private boolean notificacoesWhatsappAtivas = false;

    private String webhookUrl;
}

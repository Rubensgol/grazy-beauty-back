package com.example.grazy_back.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO para configurações visuais do tenant (onboarding e configuração).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoTenantRequest 
{
    // Cores
    private String corPrimaria;
    private String corSecundaria;
    private String corFundo;
    private String corTexto;

    // Branding
    private String logoUrl;
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
    private String horarioFuncionamento;

    // Configurações de agendamento
    private Integer intervaloAgendamentoMinutos;
    private Integer antecedenciaMinimaHoras;
    private Integer antecedenciaMaximaDias;

    // Notificações
    private Boolean notificacoesEmailAtivas;
    private Boolean notificacoesWhatsappAtivas;
    private String webhookUrl;
}

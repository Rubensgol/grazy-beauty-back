package com.example.grazy_back.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO de resposta com configurações públicas do tenant.
 * Usado pelo frontend para aplicar o white-label.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoTenantResponse 
{
    private Long tenantId;
    private String nomeNegocio;
    private String subdominio;
    
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

    // Horário de funcionamento
    private String horarioFuncionamento;

    // Configurações de agendamento
    private Integer intervaloAgendamentoMinutos;
    private Integer antecedenciaMinimaHoras;
    private Integer antecedenciaMaximaDias;

    // Status do tenant
    private boolean onboardingCompleto;
    private String plano;
    private String status; // ATIVO, SUSPENSO, TRIAL, etc
    private String motivoSuspensao; // Mensagem para exibir quando suspenso
}

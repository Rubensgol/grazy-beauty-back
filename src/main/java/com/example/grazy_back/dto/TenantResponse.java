package com.example.grazy_back.dto;

import java.time.Instant;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.example.grazy_back.enums.PlanoEnum;
import com.example.grazy_back.enums.StatusTenantEnum;

/**
 * DTO de resposta com informações do tenant criado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantResponse 
{
    private Long id;
    private String nomeNegocio;
    private String subdominio;
    private String dominioCustomizado;
    private String emailAdmin;
    private String nomeAdmin;
    private String telefoneAdmin;
    private PlanoEnum plano;
    private StatusTenantEnum status;
    private boolean ativo;
    private boolean onboardingCompleto;
    private Integer agendamentosNoMes;
    private Integer limiteAgendamentosMes;
    private Instant criadoEm;
    private Instant atualizadoEm;
    private Instant suspensaoEm;
    private String motivoSuspensao;
    private String urlAcesso;
    private String senhaProvisoria; // Apenas na resposta de criação
    
    // Configurações visuais
    private String corPrimaria;
    private String logoUrl;
}

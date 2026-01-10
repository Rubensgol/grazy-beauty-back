package com.example.grazy_back.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO para dados do onboarding (primeiro acesso).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingRequest 
{
    // Passo 1: Cores da marca
    private String corPrimaria;
    private String corSecundaria;

    // Passo 2: Logo
    private String logoUrl;

    // Passo 3: Primeiro serviço (opcional aqui, pode criar via ServicoController)
    private String nomeServico;
    private String descricaoServico;
    private Double precoServico;
    private Integer duracaoServico;

    // Contato básico
    private String telefone;
    private String whatsapp;

    // Nova senha (trocar a provisória)
    private String novaSenha;
}

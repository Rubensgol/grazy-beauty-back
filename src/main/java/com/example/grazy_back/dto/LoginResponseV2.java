package com.example.grazy_back.dto;

import java.time.Instant;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.example.grazy_back.enums.RoleEnum;

/**
 * DTO de resposta de login com informações do usuário e tenant.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseV2 
{
    private String token;
    private Long usuarioId;
    private String nome;
    private String email;
    private RoleEnum role;
    
    // Informações do tenant (null para SUPER_ADMIN)
    private Long tenantId;
    private String tenantNome;
    private String tenantSubdominio;
    
    private boolean primeiroAcesso;
    private boolean onboardingCompleto;
    
    private Instant expiresAt;
}

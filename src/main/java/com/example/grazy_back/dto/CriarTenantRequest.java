package com.example.grazy_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.example.grazy_back.enums.PlanoEnum;

/**
 * DTO para criar um novo tenant (cliente) no sistema.
 * Usado na rota /admin/master pelo Super Admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriarTenantRequest 
{
    @NotBlank(message = "Nome do negócio é obrigatório")
    private String nomeNegocio;

    @NotBlank(message = "Subdomínio é obrigatório")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomínio deve conter apenas letras minúsculas, números e hífens")
    private String subdominio;

    @NotBlank(message = "Email do administrador é obrigatório")
    @Email(message = "Email inválido")
    private String emailAdmin;

    @NotBlank(message = "Nome do administrador é obrigatório")
    private String nomeAdmin;

    private String telefoneAdmin;

    @Builder.Default
    private PlanoEnum plano = PlanoEnum.BASICO;

    private String dominioCustomizado;

    private String senhaProvisoria;
}

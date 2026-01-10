package com.example.grazy_back.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.example.grazy_back.enums.RoleEnum;

/**
 * Representa um usuário do sistema.
 * SUPER_ADMIN: Administrador do sistema (eu)
 * TENANT_ADMIN: Administrador de um negócio (ex: dono da barbearia)
 * TENANT_USER: Funcionário de um negócio
 */
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String nome;

    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoleEnum role = RoleEnum.TENANT_ADMIN;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Builder.Default
    private boolean primeiroAcesso = true;

    @Builder.Default
    private Instant criadoEm = Instant.now();

    private Instant ultimoLogin;

    private String tokenRecuperacaoSenha;
    
    private Instant tokenExpiracao;
}

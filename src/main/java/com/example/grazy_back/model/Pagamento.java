package com.example.grazy_back.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.grazy_back.enums.StatusPagamentoEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade para armazenar pagamentos/cobranças de assinatura dos tenants
 */
@Data
@Entity
@Table(name = "pagamentos")
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Column(nullable = false)
    private BigDecimal valor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamentoEnum status;
    
    @Column(name = "mercado_pago_id")
    private String mercadoPagoId; // ID do pagamento no Mercado Pago
    
    @Column(name = "mercado_pago_preference_id")
    private String mercadoPagoPreferenceId; // ID da preferência de pagamento
    
    @Column(name = "link_pagamento", columnDefinition = "TEXT")
    private String linkPagamento; // Link para o checkout do Mercado Pago
    
    @Column(name = "mes_referencia", nullable = false)
    private Integer mesReferencia; // 1-12
    
    @Column(name = "ano_referencia", nullable = false)
    private Integer anoReferencia;
    
    @Column(name = "data_vencimento")
    private LocalDateTime dataVencimento;
    
    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;
    
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;
    
    @Column(name = "enviado_whatsapp")
    private Boolean enviadoWhatsapp = false;
    
    @Column(name = "enviado_email")
    private Boolean enviadoEmail = false;
    
    @Column(name = "data_envio_cobranca")
    private LocalDateTime dataEnvioCobranca;
    
    @Column(columnDefinition = "TEXT")
    private String observacoes;
}

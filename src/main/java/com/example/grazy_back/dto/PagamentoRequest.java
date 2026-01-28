package com.example.grazy_back.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para criar um pagamento/cobrança
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoRequest {
    private Long tenantId;
    private BigDecimal valor;
    private Integer mesReferencia;
    private Integer anoReferencia;
    private String descricao;
}

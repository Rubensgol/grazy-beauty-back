package com.example.grazy_back.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuração do dia de pagamento do tenant
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaPagamentoRequest {
    @Min(value = 1, message = "Dia de pagamento deve ser no mínimo 1")
    @Max(value = 31, message = "Dia de pagamento deve ser no máximo 31")
    private Integer diaPagamento; // 1-31
    
    private Boolean enviarWhatsapp;
    private Boolean enviarEmail;
}

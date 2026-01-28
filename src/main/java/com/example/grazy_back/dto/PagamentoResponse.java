package com.example.grazy_back.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response de criação de pagamento com link do Mercado Pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoResponse {
    private Long pagamentoId;
    private String linkPagamento;
    private String mercadoPagoPreferenceId;
    private String qrCodeBase64; // QR Code para Pix
    private String qrCode; // Código Pix copia e cola
    private String status;
    private String mensagem;
    
    public Boolean getSucesso()
    {
        return "sucesso".equals(status);
    }
    
    public static PagamentoResponse erro(String mensagem) 
    {
        PagamentoResponse response = new PagamentoResponse();
        response.setStatus("erro");
        response.setMensagem(mensagem);
        return response;
    }
    
    public static PagamentoResponse sucesso(Long pagamentoId, String linkPagamento, String preferenceId) 
    {
        PagamentoResponse response = new PagamentoResponse();
        response.setPagamentoId(pagamentoId);
        response.setLinkPagamento(linkPagamento);
        response.setMercadoPagoPreferenceId(preferenceId);
        response.setStatus("sucesso");
        response.setMensagem("Pagamento criado com sucesso");
        return response;
    }
}

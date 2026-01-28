package com.example.grazy_back.enums;

/**
 * Status do pagamento
 */
public enum StatusPagamentoEnum 
{
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    REJEITADO("Rejeitado"),
    CANCELADO("Cancelado"),
    REEMBOLSADO("Reembolsado"),
    EM_PROCESSAMENTO("Em Processamento");

    private final String descricao;

    StatusPagamentoEnum(String descricao) 
    {
        this.descricao = descricao;
    }

    public String getDescricao() 
    {
        return descricao;
    }
}

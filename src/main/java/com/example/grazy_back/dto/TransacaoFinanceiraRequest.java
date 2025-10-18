package com.example.grazy_back.dto;

import com.example.grazy_back.enums.TipoTransacaoEnum;

import lombok.Data;

/**
 *
 * @author Rubens
 */
@Data
public class TransacaoFinanceiraRequest 
{
    private Long id;
    private String descricao;
    private Double valor;
    private TipoTransacaoEnum tipo;
    private String data;
}

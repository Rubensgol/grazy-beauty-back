package com.example.grazy_back.model;

import java.util.Date;

import com.example.grazy_back.enums.TipoTransacaoEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * @author Rubens
 */

@Data
@Entity
public class TransacaoFinanceira 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double valor;
    private String descricao;
    private Date data;

    @Enumerated(EnumType.STRING)
    private TipoTransacaoEnum tipo;
}

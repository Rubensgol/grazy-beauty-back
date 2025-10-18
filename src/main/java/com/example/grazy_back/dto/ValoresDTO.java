package com.example.grazy_back.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValoresDTO 
{
    private double receitasTotais;
    private double despesasTotais;
    private double lucroLiquido;

    public ValoresDTO(double receitasTotais, double despesasTotais, double lucroLiquido)
    {
        this.despesasTotais = despesasTotais;
        this.receitasTotais = receitasTotais;
        this.lucroLiquido = lucroLiquido;
    }
}

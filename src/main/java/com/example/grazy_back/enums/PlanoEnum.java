package com.example.grazy_back.enums;

/**
 * Planos disponíveis para tenants.
 */
public enum PlanoEnum 
{
    GRATUITO("Gratuito", 0, 50),
    BASICO("Básico", 4990, 200),    // R$ 49,90
    PRO("Pro", 9990, 500),          // R$ 99,90
    ENTERPRISE("Enterprise", 19990, -1); // -1 = ilimitado

    private final String nome;
    private final int precoCentavos;
    private final int limiteAgendamentosMes;

    PlanoEnum(String nome, int precoCentavos, int limiteAgendamentosMes) 
    {
        this.nome = nome;
        this.precoCentavos = precoCentavos;
        this.limiteAgendamentosMes = limiteAgendamentosMes;
    }

    public String getNome() 
    {
        return nome;
    }

    public int getPrecoCentavos() 
    {
        return precoCentavos;
    }

    public int getLimiteAgendamentosMes() 
    {
        return limiteAgendamentosMes;
    }

    public boolean temDominioCustomizado() 
    {
        return this == PRO || this == ENTERPRISE;
    }
}

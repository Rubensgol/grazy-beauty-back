package com.example.grazy_back.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum para tipo de transação. Suporta desserialização case-insensitive (ex: "receita").
 *
 * @author Rubens
 */
public enum TipoTransacaoEnum 
{
    RECEITA,
    DESPESA;

    @JsonCreator
    public static TipoTransacaoEnum fromString(String key) 
    {
        if (key == null) 
            return null;

        String trimmed = key.trim();

        if (trimmed.isEmpty()) 
            return null;

        try 
        {
            return TipoTransacaoEnum.valueOf(trimmed.toUpperCase());
        } 
        catch (IllegalArgumentException ex) 
        {
            throw new IllegalArgumentException("Valor de TipoTransacaoEnum inválido: '" + key + "'. Valores válidos: RECEITA, DESPESA");
        }
    }

    @JsonValue
    public String toValue() 
    {
        return this.name();
    }
}

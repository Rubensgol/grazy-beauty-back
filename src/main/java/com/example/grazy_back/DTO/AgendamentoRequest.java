package com.example.grazy_back.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgendamentoRequest 
{
    private Long servicoId;
    private Long usuarioId;
    // ISO 8601 ex: 2025-08-31T15:30:00
    private String dataHora;
    private String obs;
}

package com.example.grazy_back.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServicoRequest 
{
    private String nome;
    private String descricao;
    private Double preco;
    private String storedFilename;
    private Integer duracaoMinutos;
}

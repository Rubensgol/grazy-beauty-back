package com.example.grazy_back.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServicoRequest 
{
    private String nome;
    private String descricao;
    private Double preco;
    @JsonAlias({"imageStoredFilename", "image_stored_filename"})
    private String storedFilename;
    private Integer duracaoMinutos;
    private Integer ordem;
}

package com.example.grazy_back.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConteudoHero 
{
    private String titulo;
    private String subtitulo;
    private String imagemUrl;
}

package com.example.grazy_back.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Conteudo 
{
    private ConteudoHero hero;
    private ConteudoAbout about;
}

package com.example.grazy_back.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConteudoAbout 
{
    private String titulo;
    private String subtitulo;
    private String texto;
    private String imagemUrl;
    private List<ConteudoStat> stats;
}

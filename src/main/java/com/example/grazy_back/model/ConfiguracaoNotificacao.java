package com.example.grazy_back.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfiguracaoNotificacao 
{
    private boolean ativo;
    private Long periodoMinutos;
    private Map<String, String> plataformas;
}

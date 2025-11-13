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
    // Resumo diário dos agendamentos
    private boolean resumoAtivo;       // habilita envio de resumo diário
    private String resumoEmail;        // email destino do resumo (se vazio tenta plataformas.get("EMAIL"))
}

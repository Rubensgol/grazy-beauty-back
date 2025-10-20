package com.example.grazy_back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.model.ConfiguracaoNotificacao;
import com.example.grazy_back.service.ConfiguracaoNotificacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/notificacao/config")
@Tag(name = "Notificações", description = "Configurações de notificação do sistema")
public class ConfiguracaoNotificacaoController 
{
    private final ConfiguracaoNotificacaoService service;

    public ConfiguracaoNotificacaoController(ConfiguracaoNotificacaoService service) 
    {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Obtém configuração de notificação")
    public ResponseEntity<ApiResposta<ConfiguracaoNotificacao>> get() 
    {
        return ResponseEntity.ok(ApiResposta.of(service.obter()));
    }

    @PutMapping
    @Operation(summary = "Atualiza configuração de notificação")
    public ResponseEntity<ApiResposta<?>> put(@RequestBody ConfiguracaoNotificacao cfg) 
    {
        try 
        {
            return ResponseEntity.ok(ApiResposta.of(service.atualizar(cfg)));
        } 
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest().body(ApiResposta.error(e.getMessage()));
        } 
        catch (Exception e) 
        {
            return ResponseEntity.internalServerError().body(ApiResposta.error("Erro ao salvar configuração"));
        }
    }
}

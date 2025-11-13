package com.example.grazy_back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.model.Conteudo;
import com.example.grazy_back.model.ConteudoAbout;
import com.example.grazy_back.model.ConteudoHero;
import com.example.grazy_back.service.ConteudoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/conteudo")
@Tag(name = "Conteúdo", description = "Gerencia conteúdo público do site (hero e about)")
public class ConteudoController 
{
    private final ConteudoService service;

    public ConteudoController(ConteudoService service) 
    {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Buscar conteúdo atual")
    public ResponseEntity<ApiResposta<Conteudo>> get() 
    {
        return ResponseEntity.ok(ApiResposta.of(service.obter()));
    }

    @PutMapping("/hero")
    @Operation(summary = "Salvar Hero")
    public ResponseEntity<ApiResposta<Conteudo>> salvarHero(@RequestBody ConteudoHero hero)
    {
        try 
        {
            return ResponseEntity.ok(ApiResposta.of(service.atualizarHero(hero)));
        } 
        catch (IllegalArgumentException e)
        {
            return ResponseEntity.badRequest().body(ApiResposta.error(e.getMessage()));
        } 
        catch (Exception e)
        {
            return ResponseEntity.internalServerError().body(ApiResposta.error("Erro ao salvar hero"));
        }
    }

    @PutMapping("/about")
    @Operation(summary = "Salvar About")
    public ResponseEntity<ApiResposta<Conteudo>> salvarAbout(@RequestBody ConteudoAbout about) 
    {
        try 
        {
            return ResponseEntity.ok(ApiResposta.of(service.atualizarAbout(about)));
        } 
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest().body(ApiResposta.error(e.getMessage()));
        } 
        catch (Exception e)
        {
            return ResponseEntity.internalServerError().body(ApiResposta.error("Erro ao salvar about"));
        }
    }
}

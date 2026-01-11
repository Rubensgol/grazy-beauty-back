package com.example.grazy_back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.model.Conteudo;
import com.example.grazy_back.model.ConteudoAbout;
import com.example.grazy_back.model.ConteudoHero;
import com.example.grazy_back.security.TenantContext;
import com.example.grazy_back.service.ConteudoService;
import com.example.grazy_back.service.TenantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/conteudo")
@Tag(name = "Conteúdo", description = "Gerencia conteúdo público do site (hero e about)")
@RequiredArgsConstructor
@Slf4j
public class ConteudoController 
{
    private final ConteudoService service;
    private final TenantService tenantService;

    /**
     * Busca conteúdo do tenant logado.
     */
    @GetMapping
    @Operation(summary = "Buscar conteúdo do tenant logado")
    public ResponseEntity<ApiResposta<Conteudo>> get() 
    {
        return ResponseEntity.ok(ApiResposta.of(service.obter()));
    }
    
    /**
     * Busca conteúdo público baseado no Host da requisição.
     * Usa o TenantFilter para identificar o tenant automaticamente.
     */
    @GetMapping("/publico")
    @Operation(summary = "Buscar conteúdo público via Host", 
               description = "Identifica o tenant pelo cabeçalho Host da requisição")
    public ResponseEntity<ApiResposta<Conteudo>> getPublico() 
    {
        Long tenantId = TenantContext.getTenantIdFromRequest();
        String subdominio = TenantContext.getSubdominio();
        
        log.info("Buscando conteúdo público - TenantId: {}, Subdomínio: {}", tenantId, subdominio);
        
        if (tenantId != null) 
        {
            Conteudo conteudo = service.obterPorTenant(tenantId);
            return ResponseEntity.ok(ApiResposta.of(conteudo));
        }
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Busca conteúdo público por subdomínio (para landing page pública).
     */
    @GetMapping("/{subdominio}")
    @Operation(summary = "Buscar conteúdo público por subdomínio")
    public ResponseEntity<ApiResposta<Conteudo>> getBySubdominio(@PathVariable String subdominio) 
    {
        return service.obterPorSubdominio(subdominio, tenantService)
            .map(conteudo -> ResponseEntity.ok(ApiResposta.of(conteudo)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca conteúdo por tenant ID (para admin).
     */
    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Buscar conteúdo por tenant ID")
    public ResponseEntity<ApiResposta<Conteudo>> getByTenantId(@PathVariable Long tenantId) 
    {
        return ResponseEntity.ok(ApiResposta.of(service.obterPorTenant(tenantId)));
    }

    @PutMapping("/hero")
    @Operation(summary = "Salvar Hero")
    public ResponseEntity<ApiResposta<Conteudo>> salvarHero(@RequestBody ConteudoHero hero)
    {
        try 
        {
            return ResponseEntity.ok(ApiResposta.of(service.atualizarHero(hero)));
        } 
        catch (IllegalArgumentException | IllegalStateException e)
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
        catch (IllegalArgumentException | IllegalStateException e) 
        {
            return ResponseEntity.badRequest().body(ApiResposta.error(e.getMessage()));
        } 
        catch (Exception e)
        {
            return ResponseEntity.internalServerError().body(ApiResposta.error("Erro ao salvar about"));
        }
    }
}

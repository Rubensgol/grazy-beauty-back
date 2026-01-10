package com.example.grazy_back.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.dto.ConfiguracaoTenantRequest;
import com.example.grazy_back.dto.ConfiguracaoTenantResponse;
import com.example.grazy_back.security.JwtAuthenticationToken;
import com.example.grazy_back.service.TenantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller para configurações do tenant.
 * Rota pública para frontend buscar config (white-label).
 * Rota autenticada para admin atualizar config.
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "Configuração Tenant", description = "Configurações visuais e de negócio do tenant")
@RequiredArgsConstructor
public class TenantConfigController 
{
    private final TenantService tenantService;

    /**
     * Busca configurações padrão (sem tenant específico).
     * Retorna configurações genéricas para o painel principal.
     */
    @GetMapping
    @Operation(summary = "Busca configurações padrão", 
               description = "Retorna configurações padrão do sistema (sem tenant específico)")
    public ResponseEntity<ApiResposta<ConfiguracaoTenantResponse>> buscarConfiguracaoPadrao() 
    {
        // Tenta pegar tenant do usuário logado
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getTenantId() != null) 
        {
            return tenantService.buscarConfiguracaoPorId(jwtAuth.getTenantId())
                .map(config -> ResponseEntity.ok(ApiResposta.of(config)))
                .orElseGet(() -> ResponseEntity.ok(ApiResposta.of(getConfigPadrao())));
        }
        
        // Retorna uma configuração padrão para o sistema principal
        return ResponseEntity.ok(ApiResposta.of(getConfigPadrao()));
    }

    /**
     * Atualiza configurações do tenant do usuário logado.
     */
    @PutMapping
    @Operation(summary = "Atualiza configurações do tenant logado", 
               description = "Atualiza cores, logo, contato usando o tenant do usuário autenticado")
    public ResponseEntity<ApiResposta<ConfiguracaoTenantResponse>> atualizarConfiguracaoLogado(
            @RequestBody ConfiguracaoTenantRequest request) 
    {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (!(auth instanceof JwtAuthenticationToken jwtAuth) || jwtAuth.getTenantId() == null) 
        {
            return ResponseEntity.status(403)
                .body(new ApiResposta<>(false, null, "Usuário não pertence a nenhum tenant", java.time.Instant.now()));
        }

        try 
        {
            ConfiguracaoTenantResponse response = tenantService.atualizarConfiguracao(jwtAuth.getTenantId(), request);
            return ResponseEntity.ok(ApiResposta.of(response, "Configurações atualizadas com sucesso"));
        } 
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest()
                .body(new ApiResposta<>(false, null, e.getMessage(), java.time.Instant.now()));
        }
    }

    private ConfiguracaoTenantResponse getConfigPadrao() 
    {
        return ConfiguracaoTenantResponse.builder()
            .nomeNegocio("Grazy Beauty")
            .corPrimaria("#3B82F6")
            .corSecundaria("#10B981")
            .corFundo("#FFFFFF")
            .corTexto("#1F2937")
            .nomeExibicao("Grazy Beauty")
            .onboardingCompleto(true)
            .plano("SISTEMA")
            .build();
    }

    /**
     * Upload de logo do tenant (arquivo).
     * Converte para base64 e salva no banco.
     */
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de logo", 
               description = "Faz upload da logo do tenant como arquivo de imagem")
    public ResponseEntity<ApiResposta<Map<String, String>>> uploadLogo(
            @RequestParam("logo") MultipartFile file) 
    {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (!(auth instanceof JwtAuthenticationToken jwtAuth) || jwtAuth.getTenantId() == null) 
        {
            return ResponseEntity.status(403)
                .body(new ApiResposta<>(false, null, "Usuário não pertence a nenhum tenant", java.time.Instant.now()));
        }

        try 
        {
            // Validar tipo de arquivo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) 
            {
                return ResponseEntity.badRequest()
                    .body(new ApiResposta<>(false, null, "Arquivo deve ser uma imagem", java.time.Instant.now()));
            }

            // Validar tamanho (2MB máximo)
            if (file.getSize() > 2 * 1024 * 1024) 
            {
                return ResponseEntity.badRequest()
                    .body(new ApiResposta<>(false, null, "Arquivo deve ter no máximo 2MB", java.time.Instant.now()));
            }

            // Converter para base64 data URL
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String logoUrl = "data:" + contentType + ";base64," + base64;

            // Salvar no tenant
            ConfiguracaoTenantRequest request = ConfiguracaoTenantRequest.builder()
                .logoUrl(logoUrl)
                .build();
            
            tenantService.atualizarConfiguracao(jwtAuth.getTenantId(), request);

            return ResponseEntity.ok(ApiResposta.of(
                Map.of("logoUrl", logoUrl), 
                "Logo atualizada com sucesso"
            ));
        } 
        catch (IOException e) 
        {
            return ResponseEntity.internalServerError()
                .body(new ApiResposta<>(false, null, "Erro ao processar arquivo: " + e.getMessage(), java.time.Instant.now()));
        }
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest()
                .body(new ApiResposta<>(false, null, e.getMessage(), java.time.Instant.now()));
        }
    }

    /**
     * Busca configurações públicas do tenant pelo subdomínio.
     * Esta rota é usada pelo frontend para aplicar white-label.
     * 
     * Exemplo de uso: Frontend detecta subdomínio "joao" e chama
     * GET /api/config/joao para buscar cores, logo, etc.
     */
    @GetMapping("/{subdominio}")
    @Operation(summary = "Busca configurações públicas", 
               description = "Retorna configurações de cores, logo e dados públicos do tenant para aplicar white-label no frontend")
    public ResponseEntity<ApiResposta<ConfiguracaoTenantResponse>> buscarConfiguracao(@PathVariable String subdominio) 
    {
        return tenantService.buscarConfiguracao(subdominio)
            .map(config -> ResponseEntity.ok(ApiResposta.of(config)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca configurações do tenant pelo ID.
     * Usado pelo admin do tenant logado.
     */
    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Busca configurações por ID", 
               description = "Retorna configurações do tenant para admin logado")
    public ResponseEntity<ApiResposta<ConfiguracaoTenantResponse>> buscarConfiguracaoPorId(@PathVariable Long tenantId) 
    {
        return tenantService.buscarConfiguracaoPorId(tenantId)
            .map(config -> ResponseEntity.ok(ApiResposta.of(config)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Atualiza configurações do tenant.
     * Usado pelo admin do tenant para personalizar o sistema.
     */
    @PutMapping("/tenant/{tenantId}")
    @Operation(summary = "Atualiza configurações", 
               description = "Atualiza cores, logo, contato e outras configurações do tenant")
    public ResponseEntity<ApiResposta<ConfiguracaoTenantResponse>> atualizarConfiguracao(
            @PathVariable Long tenantId,
            @RequestBody ConfiguracaoTenantRequest request) 
    {
        try 
        {
            ConfiguracaoTenantResponse response = tenantService.atualizarConfiguracao(tenantId, request);
            return ResponseEntity.ok(ApiResposta.of(response, "Configurações atualizadas com sucesso"));
        } 
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest()
                .body(new ApiResposta<>(false, null, e.getMessage(), java.time.Instant.now()));
        }
    }
}

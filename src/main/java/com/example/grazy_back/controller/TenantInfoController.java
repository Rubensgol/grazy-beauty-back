package com.example.grazy_back.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.security.TenantContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/tenant")
@Tag(name = "Tenant Info", description = "Informações sobre o tenant da requisição")
@Slf4j
public class TenantInfoController 
{
    /**
     * Retorna informações sobre o tenant identificado na requisição.
     * Útil para debug e validação do TenantFilter.
     */
    @GetMapping("/info")
    @Operation(summary = "Obter informações do tenant", 
               description = "Retorna o tenant identificado pelo cabeçalho Host")
    public ResponseEntity<ApiResposta<Map<String, Object>>> getTenantInfo(HttpServletRequest request) 
    {
        Map<String, Object> info = new HashMap<>();
        
        // Informações do request
        String host = request.getHeader("Host");
        info.put("host", host);
        info.put("requestURI", request.getRequestURI());
        info.put("method", request.getMethod());
        
        // Informações do tenant identificado pelo filtro
        Long tenantId = TenantContext.getTenantIdFromRequest();
        String subdominio = TenantContext.getSubdominio();
        
        info.put("tenantId", tenantId);
        info.put("subdominio", subdominio);
        info.put("tenantIdentificado", tenantId != null);
        
        // Informações do tenant do usuário autenticado (se houver)
        Long authTenantId = TenantContext.getCurrentTenantId();
        info.put("authTenantId", authTenantId);
        info.put("usuarioAutenticado", authTenantId != null);
        
        log.info("Tenant Info - Host: {}, TenantId: {}, Subdomínio: {}", 
            host, tenantId, subdominio);
        
        return ResponseEntity.ok(ApiResposta.of(info));
    }
    
    /**
     * Endpoint público para verificar o tenant sem autenticação.
     */
    @GetMapping("/publico/verificar")
    @Operation(summary = "Verificar tenant público", 
               description = "Verifica o tenant identificado sem necessidade de autenticação")
    public ResponseEntity<ApiResposta<Map<String, Object>>> verificarTenantPublico(HttpServletRequest request) 
    {
        Map<String, Object> resultado = new HashMap<>();
        
        String host = request.getHeader("Host");
        Long tenantId = TenantContext.getTenantIdFromRequest();
        String subdominio = TenantContext.getSubdominio();
        
        resultado.put("host", host);
        resultado.put("tenantEncontrado", tenantId != null);
        
        if (tenantId != null) 
        {
            resultado.put("tenantId", tenantId);
            resultado.put("subdominio", subdominio);
            resultado.put("mensagem", "Tenant identificado com sucesso pelo Host");
        } 
        else 
        {
            resultado.put("mensagem", "Nenhum tenant identificado para este Host");
        }
        
        return ResponseEntity.ok(ApiResposta.of(resultado));
    }
}

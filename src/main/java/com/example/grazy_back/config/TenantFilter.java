package com.example.grazy_back.config;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.grazy_back.model.Tenant;
import com.example.grazy_back.service.TenantService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro que intercepta o cabeçalho Host para identificar o tenant.
 * Extrai o subdomínio ou domínio customizado da requisição e armazena
 * o tenant no contexto da requisição.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class TenantFilter extends OncePerRequestFilter
{
    private final TenantService tenantService;
    
    @Value("${app.domain:grazybeauty.com.br}")
    private String appDomain;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        String host = request.getHeader("Host");
        
        if (host != null && ! host.isEmpty()) 
        {
            log.debug("Processando requisição para Host: {}", host);
            
            // Remove porta se existir (ex: localhost:8080 -> localhost)
            host = host.split(":")[0];
            
            Optional<Tenant> tenant = identificarTenant(host);
            
            if (tenant.isPresent())
            {
                // Armazena o tenant no request para uso posterior
                request.setAttribute("tenant", tenant.get());
                request.setAttribute("tenantId", tenant.get().getId());
                request.setAttribute("subdominio", tenant.get().getSubdominio());
                
                log.debug("Tenant identificado: {} (ID: {})", 
                    tenant.get().getSubdominio(), 
                    tenant.get().getId());
            }
            else
                log.debug("Nenhum tenant identificado para o host: {}", host);
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Identifica o tenant baseado no host da requisição.
     * Tenta primeiro por domínio customizado, depois por subdomínio.
     */
    private Optional<Tenant> identificarTenant(String host) 
    {
        // 1. Tenta buscar por domínio customizado
        Optional<Tenant> tenant = tenantService.buscarPorDominioCustomizado(host);
        
        if (tenant.isPresent()) 
        {
            log.debug("Tenant encontrado por domínio customizado: {}", host);
            return tenant;
        }
        
        // 2. Se for o domínio principal (grazybeauty.com.br ou www.grazybeauty.com.br)
        // tenta buscar um tenant padrão
        if (host.equals(appDomain) || host.equals("www." + appDomain))
        {
            log.debug("Domínio principal acessado: {}", host);
            // Tenta buscar tenant com subdomínio "default" ou "www"
            tenant = tenantService.buscarPorSubdominio("default")
                .or(() -> tenantService.buscarPorSubdominio("www"));
            
            if (tenant.isPresent())
            {
                log.debug("Tenant padrão encontrado para domínio principal");
                return tenant;
            }
        }
        
        // 3. Tenta extrair subdomínio
        if (host.endsWith("." + appDomain)) 
        {
            String subdominio = extrairSubdominio(host);
            
            if (subdominio != null && !subdominio.isEmpty()) 
            {
                tenant = tenantService.buscarPorSubdominio(subdominio);
                
                if (tenant.isPresent())
                {
                    log.debug("Tenant encontrado por subdomínio: {}", subdominio);
                    return tenant;
                }
            }
        }
        
        // 4. Para localhost/desenvolvimento, tenta como subdomínio direto
        if (host.equals("localhost") || host.startsWith("127.0.0.1") || host.startsWith("192.168.")) 
        {
            log.debug("Host local detectado, não identificando tenant automaticamente");
            return Optional.empty();
        }
        
        log.warn("Tenant não encontrado para host: {}", host);
        return Optional.empty();
    }

    /**
     * Extrai o subdomínio do host.
     * Ex: cliente.seusistema.com -> cliente
     */
    private String extrairSubdominio(String host)
    {
        if (host.endsWith("." + appDomain)) {
            String subdominio = host.substring(0, host.length() - appDomain.length() - 1);
            
            // Remove www se existir
            if (subdominio.startsWith("www.")) {
                subdominio = subdominio.substring(4);
            }
            
            return subdominio;
        }
        
        return null;
    }
}

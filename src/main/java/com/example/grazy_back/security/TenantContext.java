package com.example.grazy_back.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilitário para obter informações do tenant do contexto de segurança.
 */
public class TenantContext 
{
    private TenantContext() {}
    
    /**
     * Obtém o tenantId do usuário logado.
     * Se não houver usuário autenticado, tenta obter do cabeçalho Host.
     * @return tenantId ou null se não encontrado
     */
    public static Long getCurrentTenantId() 
    {
        // 1. Tenta obter do usuário autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth instanceof JwtAuthenticationToken jwtAuth) 
        {
            return jwtAuth.getTenantId();
        }
        
        // 2. Tenta obter do request (identificado pelo TenantFilter)
        return getTenantIdFromRequest();
    }
    
    /**
     * Obtém o tenantId identificado pelo TenantFilter a partir do cabeçalho Host.
     * @return tenantId ou null se não encontrado
     */
    public static Long getTenantIdFromRequest() 
    {
        try 
        {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) 
            {
                HttpServletRequest request = attributes.getRequest();
                Object tenantId = request.getAttribute("tenantId");
                
                if (tenantId instanceof Long) 
                {
                    return (Long) tenantId;
                }
            }
        } 
        catch (Exception e) 
        {
            // Ignora se não estiver em contexto de request
        }
        
        return null;
    }
    
    /**
     * Obtém o subdomínio identificado pelo TenantFilter.
     * @return subdomínio ou null se não encontrado
     */
    public static String getSubdominio() 
    {
        try 
        {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) 
            {
                HttpServletRequest request = attributes.getRequest();
                Object subdominio = request.getAttribute("subdominio");
                
                if (subdominio instanceof String) 
                {
                    return (String) subdominio;
                }
            }
        } 
        catch (Exception e) 
        {
            // Ignora se não estiver em contexto de request
        }
        
        return null;
    }
    
    /**
     * Obtém o tenantId, lançando exceção se não encontrado.
     * @return tenantId
     * @throws IllegalStateException se não houver tenant
     */
    public static Long requireTenantId() 
    {
        Long tenantId = getCurrentTenantId();
        
        if (tenantId == null) 
        {
            throw new IllegalStateException("Usuário não pertence a nenhum tenant");
        }
        
        return tenantId;
    }
    
    /**
     * Verifica se o usuário é SUPER_ADMIN.
     */
    public static boolean isSuperAdmin() 
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth instanceof JwtAuthenticationToken jwtAuth) 
        {
            return jwtAuth.isSuperAdmin();
        }
        
        return false;
    }
}

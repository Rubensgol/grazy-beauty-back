package com.example.grazy_back.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitário para obter informações do tenant do contexto de segurança.
 */
public class TenantContext 
{
    private TenantContext() {}
    
    /**
     * Obtém o tenantId do usuário logado.
     * @return tenantId ou null se não autenticado ou SUPER_ADMIN
     */
    public static Long getCurrentTenantId() 
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth instanceof JwtAuthenticationToken jwtAuth) 
        {
            return jwtAuth.getTenantId();
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

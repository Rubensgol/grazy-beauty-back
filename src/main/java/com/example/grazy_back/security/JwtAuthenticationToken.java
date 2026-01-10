package com.example.grazy_back.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Token de autenticação JWT com informações de role e tenant.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken 
{
    private final String username;
    private final String role;
    private final Long tenantId;

    public JwtAuthenticationToken(String username, Collection<? extends GrantedAuthority> authorities, 
                                  String role, Long tenantId) 
    {
        super(authorities);
        this.username = username;
        this.role = role;
        this.tenantId = tenantId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() 
    {
        return null;
    }

    @Override
    public Object getPrincipal() 
    {
        return username;
    }

    public String getUsername() 
    {
        return username;
    }

    public String getRole() 
    {
        return role;
    }

    public Long getTenantId() 
    {
        return tenantId;
    }

    public boolean isSuperAdmin() 
    {
        return "SUPER_ADMIN".equals(role);
    }

    public boolean isTenantAdmin() 
    {
        return "TENANT_ADMIN".equals(role);
    }
}

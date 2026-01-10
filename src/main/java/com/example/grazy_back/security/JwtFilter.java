package com.example.grazy_back.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtFilter extends OncePerRequestFilter 
{
    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) 
    {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException 
    {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) 
        {
            String token = auth.substring(7);
            if (jwtUtil.validate(token))
            {
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);
                Long tenantId = jwtUtil.getTenantId(token);
                
                // Cria lista de authorities baseado na role
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (role != null) 
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                else 
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                
                // Cria autenticação com informações adicionais
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    username, 
                    authorities, 
                    role, 
                    tenantId
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}

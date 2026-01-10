package com.example.grazy_back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class CorsConfig implements WebMvcConfigurer 
{
    @Value("${app.domain:grazybeauty.com.br}")
    private String appDomain;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry)
    {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        // Desenvolvimento local
                        "http://localhost:*",
                        "https://localhost:*",
                        // IP direto
                        "http://144.22.228.196",
                        "https://144.22.228.196",
                        // Domínio principal
                        "http://grazybeauty.com.br",
                        "http://www.grazybeauty.com.br",
                        "https://grazybeauty.com.br",
                        "https://www.grazybeauty.com.br",
                        // Subdomínios (para multi-tenant)
                        "http://*.grazybeauty.com.br",
                        "https://*.grazybeauty.com.br"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry)
    {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}

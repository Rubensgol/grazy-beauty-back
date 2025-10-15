package com.example.grazy_back.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class CorsConfig implements WebMvcConfigurer 
{
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry)
    {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:8000",
                        "http://144.22.228.196",
                        "http://grazybeauty.com.br",
                        "http://www.grazybeauty.com.br",
                        "https://144.22.228.196",
                        "https://grazybeauty.com.br",
                        "https://www.grazybeauty.com.br",
                        "https://localhost",
                        "http://localhost"      
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
    
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry)
    {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}

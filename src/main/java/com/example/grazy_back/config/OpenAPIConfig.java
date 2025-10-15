package com.example.grazy_back.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig 
{
    @Bean
    public OpenAPI apiInfo() 
    {
        return new OpenAPI()
                .info(new Info()
                        .title("Salao de Beleza API")
                        .version("v1")
                        .description("API do back-end do salão de beleza"));
    }
}

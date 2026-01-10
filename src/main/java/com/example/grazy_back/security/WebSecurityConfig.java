package com.example.grazy_back.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig 
{
    private final JwtUtil jwtUtil;

    public WebSecurityConfig(JwtUtil jwtUtil) 
    {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public PasswordEncoder passwordEncoder() 
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception 
    {
        JwtFilter jwtFilter = new JwtFilter(jwtUtil);

        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    // Permitir OPTIONS para CORS
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    
                    // Rotas públicas de autenticação
                    .requestMatchers("/api/auth/**").permitAll()
                    
                    // Rotas públicas de configuração (white-label)
                    .requestMatchers(HttpMethod.GET, "/api/config/**").permitAll()
                    
                    // Rotas públicas de conteúdo e serviços
                    .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/servicos/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/conteudo/**").permitAll()
                    
                    // Swagger e Actuator
                    .requestMatchers(
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html").permitAll()
                    
                    // Rotas do Super Admin (requer role SUPER_ADMIN)
                    .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                    
                    // Onboarding requer autenticação
                    .requestMatchers("/api/onboarding/**").authenticated()
                    
                    // Todas outras rotas de API requerem autenticação
                    .requestMatchers("/api/**").authenticated()
                    
                    .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

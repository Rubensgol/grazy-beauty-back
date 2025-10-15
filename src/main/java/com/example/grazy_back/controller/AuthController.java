package com.example.grazy_back.controller;

import com.example.grazy_back.dto.LoginRequest;
import com.example.grazy_back.dto.LoginResponse;
import com.example.grazy_back.security.JwtUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e emissão de token JWT")
public class AuthController 
{
    @Value("${app.admin.username:admin}")
    private String adminUser;

    @Value("${app.admin.password:admin}")
    private String adminPass;

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) 
    {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza login", description = "Valida credenciais do administrador e retorna um token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Requisição inválida (faltando usuário ou senha)", content = @Content),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content)
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest req) 
    {
        if (req.getUsername() == null || req.getSenha() == null)
            return ResponseEntity.badRequest().build();
        
        if (req.getUsername().equals(adminUser) && req.getSenha().equals(adminPass)) 
        {
            String token = jwtUtil.generateToken(adminUser);
            return ResponseEntity.ok(new LoginResponse(token));
        }

        return ResponseEntity.status(401).build();
    }
}

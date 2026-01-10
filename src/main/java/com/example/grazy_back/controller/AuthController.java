package com.example.grazy_back.controller;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.dto.LoginRequest;
import com.example.grazy_back.dto.LoginResponse;
import com.example.grazy_back.dto.LoginResponseV2;
import com.example.grazy_back.dto.ValidationResponse;
import com.example.grazy_back.security.JwtUtil;
import com.example.grazy_back.service.AuthService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e emissão de token JWT")
@RequiredArgsConstructor
public class AuthController 
{
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Value("${app.admin.username:admin}")
    private String adminUser;

    @Value("${app.admin.password:admin}")
    private String adminPass;

    /**
     * Login V2 - Autenticação com banco de dados (tenants).
     */
    @PostMapping("/login/v2")
    @Operation(summary = "Login V2", description = "Autentica usuário do sistema multi-tenant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    public ResponseEntity<ApiResposta<LoginResponseV2>> loginV2(@RequestBody LoginRequest req) 
    {
        if (req.getUsername() == null || req.getSenha() == null)
        {
            return ResponseEntity.badRequest()
                .body(new ApiResposta<>(false, null, "Email e senha são obrigatórios", java.time.Instant.now()));
        }

        return authService.autenticar(req.getUsername(), req.getSenha())
            .map(response -> ResponseEntity.ok(ApiResposta.of(response, "Login realizado com sucesso")))
            .orElse(ResponseEntity.status(401)
                .body(new ApiResposta<>(false, null, "Credenciais inválidas ou conta inativa", java.time.Instant.now())));
    }

    /**
     * Login original (compatibilidade com código antigo - admin fixo).
     */
    @PostMapping("/login")
    @Operation(summary = "Login (legado)", description = "Valida credenciais do administrador fixo e retorna um token JWT")
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
        
        // Primeiro tenta autenticar via banco (novo sistema)
        var authResult = authService.autenticar(req.getUsername(), req.getSenha());
        if (authResult.isPresent()) 
        {
            // Retorna resposta antiga para compatibilidade
            return ResponseEntity.ok(new LoginResponse(authResult.get().getToken()));
        }

        // Fallback para admin fixo (compatibilidade) - GERA TOKEN COM ROLE SUPER_ADMIN
        if (req.getUsername().equals(adminUser) && req.getSenha().equals(adminPass)) 
        {
            String token = jwtUtil.generateToken(adminUser, "SUPER_ADMIN", null);
            return ResponseEntity.ok(new LoginResponse(token));
        }

        return ResponseEntity.status(401).build();
    }

    @GetMapping("/validate")
    @Operation(summary = "Valida token JWT", description = "Recebe um token JWT no header Authorization e valida se está válido")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token válido", content = @Content),
        @ApiResponse(responseCode = "401", description = "Token inválido ou expirado", content = @Content)
    })
    public ResponseEntity<?> validate(@RequestHeader(value = "Authorization", required = false) String authHeader)
    {
        if (authHeader == null || ! authHeader.startsWith("Bearer "))
            return ResponseEntity.status(401).body("Token não fornecido ou formato inválido");

        String token = authHeader.substring(7);

        try 
        {
            if (jwtUtil.validate(token)) 
            {
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);
                Long tenantId = jwtUtil.getTenantId(token);
                
                return ResponseEntity.ok().body(ValidationResponse.builder()
                    .valid(true)
                    .message("Token válido")
                    .username(username)
                    .role(role)
                    .tenantId(tenantId)
                    .build());
            }

            return ResponseEntity.status(401).body(new ValidationResponse(false, "Token inválido", null));
        }
        catch (Exception e)
        {
            return ResponseEntity.status(401).body(new ValidationResponse(false, "Token inválido ou expirado: " + e.getMessage(), null));
        }
    }
}

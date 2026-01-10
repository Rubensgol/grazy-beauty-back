package com.example.grazy_back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.repository.UsuarioRepository;
import com.example.grazy_back.security.JwtAuthenticationToken;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.Data;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Controller para gerenciamento de usuários.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários", description = "Gerenciamento de usuários")
@RequiredArgsConstructor
public class UsuarioController 
{
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retorna dados do usuário logado.
     */
    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado", description = "Retorna informações do usuário autenticado")
    public ResponseEntity<ApiResposta<UsuarioResponse>> getMe() 
    {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) 
        {
            return ResponseEntity.status(401)
                .body(new ApiResposta<>(false, null, "Não autenticado", java.time.Instant.now()));
        }

        String email = auth.getName();
        
        // Busca usuário no banco
        return usuarioRepository.findByEmail(email)
            .map(usuario -> {
                UsuarioResponse response = new UsuarioResponse();
                response.setId(usuario.getId());
                response.setNome(usuario.getNome());
                response.setEmail(usuario.getEmail());
                response.setTelefone(usuario.getTelefone());
                response.setRole(usuario.getRole().name());
                response.setPrimeiroAcesso(usuario.isPrimeiroAcesso());
                
                if (usuario.getTenant() != null) 
                {
                    response.setTenantId(usuario.getTenant().getId());
                    response.setTenantNome(usuario.getTenant().getNomeNegocio());
                    response.setTenantSubdominio(usuario.getTenant().getSubdominio());
                }
                
                return ResponseEntity.ok(ApiResposta.of(response));
            })
            .orElseGet(() -> {
                // Fallback para admin fixo (sem registro no banco)
                if (auth instanceof JwtAuthenticationToken jwtAuth) 
                {
                    UsuarioResponse response = new UsuarioResponse();
                    response.setNome(email);
                    response.setEmail(email);
                    response.setRole(jwtAuth.getRole() != null ? jwtAuth.getRole() : "SUPER_ADMIN");
                    response.setPrimeiroAcesso(false);
                    return ResponseEntity.ok(ApiResposta.of(response));
                }
                
                // Admin fixo sem JwtAuthenticationToken
                UsuarioResponse response = new UsuarioResponse();
                response.setNome(email);
                response.setEmail(email);
                response.setRole("ADMIN");
                response.setPrimeiroAcesso(false);
                return ResponseEntity.ok(ApiResposta.of(response));
            });
    }

    /**
     * Atualiza dados do usuário logado.
     */
    @PutMapping("/me")
    @Operation(summary = "Atualiza usuário logado", description = "Atualiza informações do usuário autenticado")
    public ResponseEntity<ApiResposta<UsuarioResponse>> updateMe(@RequestBody UpdateUsuarioRequest request) 
    {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) 
        {
            return ResponseEntity.status(401)
                .body(new ApiResposta<>(false, null, "Não autenticado", java.time.Instant.now()));
        }

        String email = auth.getName();
        
        return usuarioRepository.findByEmail(email)
            .map(usuario -> {
                if (request.getNome() != null) usuario.setNome(request.getNome());
                if (request.getTelefone() != null) usuario.setTelefone(request.getTelefone());
                
                usuario = usuarioRepository.save(usuario);
                
                UsuarioResponse response = new UsuarioResponse();
                response.setId(usuario.getId());
                response.setNome(usuario.getNome());
                response.setEmail(usuario.getEmail());
                response.setTelefone(usuario.getTelefone());
                response.setRole(usuario.getRole().name());
                
                if (usuario.getTenant() != null) 
                {
                    response.setTenantId(usuario.getTenant().getId());
                    response.setTenantNome(usuario.getTenant().getNomeNegocio());
                }
                
                return ResponseEntity.ok(ApiResposta.of(response, "Dados atualizados com sucesso"));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // DTOs internos
    @Data
    public static class UsuarioResponse 
    {
        private Long id;
        private String nome;
        private String email;
        private String telefone;
        private String role;
        private boolean primeiroAcesso;
        private Long tenantId;
        private String tenantNome;
        private String tenantSubdominio;
    }

    @Data
    public static class UpdateUsuarioRequest 
    {
        private String nome;
        private String telefone;
    }

    @Data
    public static class ChangePasswordRequest 
    {
        private String senhaAtual;
        private String novaSenha;
    }

    /**
     * Altera a senha do usuário logado.
     */
    @PutMapping("/me/password")
    @Operation(summary = "Alterar senha", description = "Altera a senha do usuário autenticado")
    public ResponseEntity<ApiResposta<String>> changePassword(@RequestBody ChangePasswordRequest request) 
    {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) 
        {
            return ResponseEntity.status(401)
                .body(new ApiResposta<>(false, null, "Não autenticado", java.time.Instant.now()));
        }

        String email = auth.getName();
        
        return usuarioRepository.findByEmail(email)
            .map(usuario -> {
                // Valida senha atual
                if (request.getSenhaAtual() != null && !request.getSenhaAtual().isBlank()) 
                {
                    if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenha())) 
                    {
                        return ResponseEntity.badRequest()
                            .body(new ApiResposta<String>(false, null, "Senha atual incorreta", java.time.Instant.now()));
                    }
                }
                
                // Valida nova senha
                if (request.getNovaSenha() == null || request.getNovaSenha().length() < 6) 
                {
                    return ResponseEntity.badRequest()
                        .body(new ApiResposta<String>(false, null, "Nova senha deve ter pelo menos 6 caracteres", java.time.Instant.now()));
                }
                
                // Atualiza senha
                usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
                usuario.setPrimeiroAcesso(false); // Marca que já alterou a senha
                usuarioRepository.save(usuario);
                
                return ResponseEntity.ok(ApiResposta.of("Senha alterada com sucesso"));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}

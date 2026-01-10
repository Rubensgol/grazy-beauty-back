package com.example.grazy_back.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.dto.OnboardingRequest;
import com.example.grazy_back.model.Usuario;
import com.example.grazy_back.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller para o processo de Onboarding (primeiro acesso).
 * 
 * Este é o "Passo 3" do plano - O Wizard de primeiro acesso:
 * - Tela 1: "Qual a cor da sua marca?"
 * - Tela 2: "Faça upload da sua logo"
 * - Tela 3: "Cadastre seu primeiro serviço"
 */
@RestController
@RequestMapping("/api/onboarding")
@Tag(name = "Onboarding", description = "Processo de configuração inicial do tenant (primeiro acesso)")
@RequiredArgsConstructor
public class OnboardingController 
{
    private final AuthService authService;

    /**
     * Processa o onboarding completo.
     * Recebe dados de todos os passos do wizard.
     */
    @PostMapping("/completar")
    @Operation(summary = "Completa onboarding", 
               description = "Processa todos os dados do wizard de primeiro acesso")
    public ResponseEntity<ApiResposta<Void>> completarOnboarding(
            @AuthenticationPrincipal String userEmail,
            @RequestBody OnboardingRequest request) 
    {
        try 
        {
            Usuario usuario = authService.buscarPorEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
            
            authService.processarOnboarding(usuario.getId(), request);
            
            return ResponseEntity.ok(ApiResposta.of(null, "Onboarding completo! Seu sistema está pronto para uso."));
        } 
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest()
                .body(new ApiResposta<>(false, null, e.getMessage(), java.time.Instant.now()));
        }
    }

    /**
     * Passo 1 do onboarding: Cores da marca.
     */
    @PostMapping("/passo/cores")
    @Operation(summary = "Passo 1: Cores", description = "Define as cores da marca do tenant")
    public ResponseEntity<ApiResposta<Void>> salvarCores(
            @AuthenticationPrincipal String userEmail,
            @RequestBody CoresRequest request) 
    {
        try 
        {
            Usuario usuario = authService.buscarPorEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

            if (usuario.getTenant() == null) 
            {
                throw new IllegalArgumentException("Usuário não pertence a nenhum tenant");
            }

            OnboardingRequest onboarding = OnboardingRequest.builder()
                .corPrimaria(request.corPrimaria())
                .corSecundaria(request.corSecundaria())
                .build();

            // Salva apenas as cores, não completa o onboarding
            authService.processarOnboarding(usuario.getId(), onboarding);
            
            return ResponseEntity.ok(ApiResposta.of(null, "Cores salvas com sucesso"));
        } 
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest()
                .body(new ApiResposta<>(false, null, e.getMessage(), java.time.Instant.now()));
        }
    }

    /**
     * Passo 2 do onboarding: Logo.
     */
    @PostMapping("/passo/logo")
    @Operation(summary = "Passo 2: Logo", description = "Define a logo do tenant")
    public ResponseEntity<ApiResposta<Void>> salvarLogo(
            @AuthenticationPrincipal String userEmail,
            @RequestBody LogoRequest request) 
    {
        try 
        {
            Usuario usuario = authService.buscarPorEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

            if (usuario.getTenant() == null) 
            {
                throw new IllegalArgumentException("Usuário não pertence a nenhum tenant");
            }

            OnboardingRequest onboarding = OnboardingRequest.builder()
                .logoUrl(request.logoUrl())
                .build();

            authService.processarOnboarding(usuario.getId(), onboarding);
            
            return ResponseEntity.ok(ApiResposta.of(null, "Logo salva com sucesso"));
        } 
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest()
                .body(new ApiResposta<>(false, null, e.getMessage(), java.time.Instant.now()));
        }
    }

    // Records para requests parciais
    record CoresRequest(String corPrimaria, String corSecundaria) {}
    record LogoRequest(String logoUrl) {}
}

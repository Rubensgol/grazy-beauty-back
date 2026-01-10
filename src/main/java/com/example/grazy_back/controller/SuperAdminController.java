package com.example.grazy_back.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.dto.CriarTenantRequest;
import com.example.grazy_back.dto.TenantResponse;
import com.example.grazy_back.service.TenantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller para Super Admin gerenciar tenants.
 * Rota: /api/admin/master e /api/admin (compatibilidade)
 * 
 * Este é o "Passo 1" do plano - A ferramenta do Super Admin.
 */
@RestController
@Tag(name = "Super Admin - Tenants", description = "Gerenciamento de tenants (clientes) pelo Super Admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController 
{
    private final TenantService tenantService;

    /**
     * Lista todos os tenants cadastrados.
     */
    @GetMapping({"/api/admin/master/tenants", "/api/admin/tenants"})
    @Operation(summary = "Lista todos os tenants", description = "Retorna lista de todos os clientes/negócios cadastrados")
    public ResponseEntity<ApiResposta<List<TenantResponse>>> listarTenants() 
    {
        List<TenantResponse> tenants = tenantService.listarTenants();
        return ResponseEntity.ok(ApiResposta.of(tenants));
    }

    /**
     * Busca um tenant por ID.
     */
    @GetMapping({"/api/admin/master/tenants/{id}", "/api/admin/tenants/{id}"})
    @Operation(summary = "Busca tenant por ID", description = "Retorna detalhes de um tenant específico")
    public ResponseEntity<ApiResposta<TenantResponse>> buscarTenant(@PathVariable Long id) 
    {
        return tenantService.buscarPorId(id)
            .map(tenant -> ResponseEntity.ok(ApiResposta.of(tenant)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cria um novo tenant (Provisionamento).
     * Este é o formulário do "Passo 1" do plano:
     * - Nome do Negócio
     * - Subdomínio Desejado
     * - Email do Dono
     * - Plano
     */
    @PostMapping({"/api/admin/master/tenants", "/api/admin/tenants"})
    @Operation(summary = "Cria novo tenant", description = "Provisiona um novo cliente/negócio no sistema")
    public ResponseEntity<ApiResposta<TenantResponse>> criarTenant(@Valid @RequestBody CriarTenantRequest request) 
    {
        try 
        {
            TenantResponse response = tenantService.criarTenant(request);
            return ResponseEntity.ok(ApiResposta.of(response, "Tenant criado com sucesso! Email de boas-vindas enviado."));
        } 
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest()
                .body(new ApiResposta<>(false, null, e.getMessage(), java.time.Instant.now()));
        }
    }

    /**
     * Suspende um tenant (por falta de pagamento, etc).
     */
    @PutMapping({"/api/admin/master/tenants/{id}/suspender", "/api/admin/tenants/{id}/suspender"})
    @Operation(summary = "Suspende tenant", description = "Suspende acesso do tenant ao sistema")
    public ResponseEntity<ApiResposta<Void>> suspenderTenant(
            @PathVariable Long id, 
            @RequestBody(required = false) SuspenderRequest request) 
    {
        String motivo = request != null ? request.motivo() : "Pagamento pendente";
        tenantService.suspenderTenant(id, motivo);
        return ResponseEntity.ok(ApiResposta.of(null, "Tenant suspenso com sucesso"));
    }

    /**
     * Reativa um tenant suspenso.
     */
    @PutMapping({"/api/admin/master/tenants/{id}/reativar", "/api/admin/tenants/{id}/reativar"})
    @Operation(summary = "Reativa tenant", description = "Reativa acesso de um tenant suspenso")
    public ResponseEntity<ApiResposta<Void>> reativarTenant(@PathVariable Long id) 
    {
        tenantService.reativarTenant(id);
        return ResponseEntity.ok(ApiResposta.of(null, "Tenant reativado com sucesso"));
    }

    /**
     * Reseta contadores mensais de todos os tenants.
     * Normalmente executado por um job no início de cada mês.
     */
    @PostMapping({"/api/admin/master/tenants/reset-contadores", "/api/admin/tenants/reset-contadores"})
    @Operation(summary = "Reseta contadores mensais", description = "Zera contadores de agendamentos de todos os tenants")
    public ResponseEntity<ApiResposta<Void>> resetarContadores() 
    {
        tenantService.resetarContadoresMensais();
        return ResponseEntity.ok(ApiResposta.of(null, "Contadores resetados com sucesso"));
    }

    // Record para request de suspensão
    record SuspenderRequest(String motivo) {}
}

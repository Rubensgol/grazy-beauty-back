package com.example.grazy_back.controller;

import com.example.grazy_back.dto.whatsapp.SendMessageRequest;
import com.example.grazy_back.dto.whatsapp.WhatsappConnectResponse;
import com.example.grazy_back.dto.whatsapp.WhatsappStatusResponse;
import com.example.grazy_back.service.EvolutionApiService;
import com.example.grazy_back.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para gerenciar conexão WhatsApp via Evolution API.
 * Cada tenant tem sua instância isolada.
 */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsappInstanceController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappInstanceController.class);

    private final EvolutionApiService evolutionApiService;

    public WhatsappInstanceController(EvolutionApiService evolutionApiService) {
        this.evolutionApiService = evolutionApiService;
    }

    /**
     * Verifica o status da conexão WhatsApp do tenant atual
     * GET /api/admin/whatsapp/status
     */
    @GetMapping("/status")
    public ResponseEntity<WhatsappStatusResponse> getStatus() {
        try {
            Long tenantId = TenantContext.getCurrentTenantId();
            log.info("[WHATSAPP] Verificando status para tenant {}", tenantId);
            
            WhatsappStatusResponse status = evolutionApiService.getConnectionStatus(tenantId);
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("[WHATSAPP] Erro ao verificar status: {}", e.getMessage());
            return ResponseEntity.ok(WhatsappStatusResponse.error(e.getMessage()));
        }
    }

    /**
     * Inicia conexão e retorna QR Code
     * POST /api/admin/whatsapp/connect
     */
    @PostMapping("/connect")
    public ResponseEntity<WhatsappConnectResponse> connect() {
        try {
            Long tenantId = TenantContext.getCurrentTenantId();
            log.info("[WHATSAPP] Iniciando conexão para tenant {}", tenantId);
            
            WhatsappConnectResponse response = evolutionApiService.connect(tenantId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[WHATSAPP] Erro ao conectar: {}", e.getMessage());
            return ResponseEntity.ok(WhatsappConnectResponse.error(e.getMessage()));
        }
    }

    /**
     * Desconecta o WhatsApp
     * POST /api/admin/whatsapp/disconnect
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        try {
            Long tenantId = TenantContext.getCurrentTenantId();
            log.info("[WHATSAPP] Desconectando tenant {}", tenantId);
            
            boolean success = evolutionApiService.disconnect(tenantId);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "WhatsApp desconectado com sucesso" : "Erro ao desconectar"
            ));
            
        } catch (Exception e) {
            log.error("[WHATSAPP] Erro ao desconectar: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Envia mensagem de teste
     * POST /api/admin/whatsapp/test
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestMessage(@RequestBody SendMessageRequest request) {
        try {
            Long tenantId = TenantContext.getCurrentTenantId();
            log.info("[WHATSAPP] Enviando mensagem de teste para {} via tenant {}", request.getPhoneNumber(), tenantId);
            
            boolean success = evolutionApiService.sendTextMessage(
                tenantId, 
                request.getPhoneNumber(), 
                request.getMessage()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Mensagem enviada com sucesso!" : "Erro ao enviar mensagem"
            ));
            
        } catch (Exception e) {
            log.error("[WHATSAPP] Erro ao enviar mensagem de teste: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Verifica se o serviço Evolution API está habilitado
     * GET /api/admin/whatsapp/enabled
     */
    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Boolean>> isEnabled() {
        return ResponseEntity.ok(Map.of("enabled", evolutionApiService.isEnabled()));
    }
}

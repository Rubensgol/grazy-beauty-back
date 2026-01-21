package com.example.grazy_back.service;

import com.example.grazy_back.dto.whatsapp.WhatsappConnectResponse;
import com.example.grazy_back.dto.whatsapp.WhatsappStatusResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço para integração com Evolution API (WhatsApp White Label).
 * Cada tenant tem sua própria instância isolada.
 */
@Service
public class EvolutionApiService {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiService.class);
    
    private final String apiUrl;
    private final String apiKey;
    private final boolean enabled;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EvolutionApiService(
            @Value("${evolution.api.url:http://localhost:8080}") String apiUrl,
            @Value("${evolution.api.key:}") String apiKey,
            @Value("${evolution.api.enabled:true}") boolean enabled) {
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        
        log.info("[EVOLUTION] Serviço inicializado - URL: {}, Enabled: {}", this.apiUrl, this.enabled);
    }

    /**
     * Gera o nome da instância baseado no tenantId
     */
    private String getInstanceName(Long tenantId) {
        return "tenant_" + tenantId;
    }

    /**
     * Cria os headers com autenticação
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", apiKey);
        return headers;
    }

    /**
     * Verifica o status da conexão WhatsApp do tenant
     */
    public WhatsappStatusResponse getConnectionStatus(Long tenantId) {
        if (!enabled) {
            // Se não está usando Evolution API, pode estar usando API oficial
            // Retornar status desconhecido/simulado
            log.debug("[EVOLUTION] API não habilitada, retornando status simulado");
            return WhatsappStatusResponse.connected("Modo Legado", "legacy");
        }

        String instanceName = getInstanceName(tenantId);
        
        try {
            String url = apiUrl + "/instance/connectionState/" + instanceName;
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            
            String state = json.path("state").asText("close");
            
            if ("open".equals(state)) {
                // Buscar informações do número conectado
                try {
                    ResponseEntity<String> profileResponse = restTemplate.exchange(
                        apiUrl + "/instance/fetchInstances?instanceName=" + instanceName, 
                        HttpMethod.GET, entity, String.class
                    );
                    JsonNode profileJson = objectMapper.readTree(profileResponse.getBody());
                    
                    // Tentar extrair o número do telefone
                    String phoneNumber = "";
                    if (profileJson.isArray() && profileJson.size() > 0) {
                        phoneNumber = profileJson.get(0).path("instance").path("owner").asText("");
                        if (phoneNumber.contains("@")) {
                            phoneNumber = phoneNumber.split("@")[0];
                        }
                    }
                    
                    return WhatsappStatusResponse.connected(formatPhoneNumber(phoneNumber), instanceName);
                } catch (Exception e) {
                    return WhatsappStatusResponse.connected("", instanceName);
                }
            } else {
                return WhatsappStatusResponse.disconnected();
            }
            
        } catch (HttpClientErrorException.NotFound e) {
            log.info("[EVOLUTION] Instância {} não existe ainda", instanceName);
            return WhatsappStatusResponse.disconnected();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Evolution API não está acessível - pode estar usando modo legado
            log.warn("[EVOLUTION] API não acessível em {}, verificando modo legado", apiUrl);
            return WhatsappStatusResponse.connected("Modo Legado (API indisponível)", "legacy");
        } catch (Exception e) {
            log.error("[EVOLUTION] Erro ao verificar status: {}", e.getMessage());
            // Em caso de erro de conexão, assumir modo legado
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || e.getMessage().contains("connect"))) {
                return WhatsappStatusResponse.connected("Modo Legado", "legacy");
            }
            return WhatsappStatusResponse.error("Erro ao verificar status: " + e.getMessage());
        }
    }

    /**
     * Cria instância e inicia conexão (gera QR Code)
     */
    public WhatsappConnectResponse connect(Long tenantId) {
        if (!enabled) {
            return WhatsappConnectResponse.error("Evolution API não está habilitada");
        }

        String instanceName = getInstanceName(tenantId);
        
        try {
            // 1. Verificar se instância já existe
            WhatsappStatusResponse currentStatus = getConnectionStatus(tenantId);
            if ("open".equals(currentStatus.getStatus())) {
                return WhatsappConnectResponse.alreadyConnected("WhatsApp já está conectado");
            }

            // 2. Criar ou recriar instância
            createInstance(instanceName);
            
            // 3. Aguardar um pouco e gerar QR Code
            Thread.sleep(1000);
            
            // 4. Obter QR Code
            String qrCode = fetchQrCode(instanceName);
            
            if (qrCode != null && !qrCode.isEmpty()) {
                return WhatsappConnectResponse.withQrCode(qrCode, instanceName);
            } else {
                return WhatsappConnectResponse.error("Não foi possível gerar o QR Code. Tente novamente.");
            }
            
        } catch (Exception e) {
            log.error("[EVOLUTION] Erro ao conectar: {}", e.getMessage(), e);
            return WhatsappConnectResponse.error("Erro ao conectar: " + e.getMessage());
        }
    }

    /**
     * Cria uma nova instância na Evolution API
     */
    private void createInstance(String instanceName) throws Exception {
        String url = apiUrl + "/instance/create";
        
        Map<String, Object> body = new HashMap<>();
        body.put("instanceName", instanceName);
        body.put("qrcode", true);
        body.put("integration", "WHATSAPP-BAILEYS");
        
        // Configurações de webhook (opcional - para receber eventos)
        // body.put("webhook", "https://seudominio.com/api/webhook/whatsapp");
        // body.put("webhookByEvents", false);
        // body.put("events", Arrays.asList("messages.upsert", "connection.update"));
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
        
        try {
            restTemplate.postForEntity(url, entity, String.class);
            log.info("[EVOLUTION] Instância {} criada com sucesso", instanceName);
        } catch (HttpClientErrorException e) {
            // Se a instância já existe, tentar deletar e recriar
            if (e.getStatusCode() == HttpStatus.CONFLICT || e.getResponseBodyAsString().contains("already")) {
                log.info("[EVOLUTION] Instância {} já existe, reconectando...", instanceName);
                // Tentar conectar diretamente
            } else {
                throw e;
            }
        }
    }

    /**
     * Busca o QR Code da instância
     */
    private String fetchQrCode(String instanceName) throws Exception {
        String url = apiUrl + "/instance/connect/" + instanceName;
        
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        
        JsonNode json = objectMapper.readTree(response.getBody());
        
        // O Evolution API retorna o QR Code em base64
        String base64 = json.path("base64").asText("");
        if (!base64.isEmpty()) {
            // Já vem como data URL ou precisa adicionar o prefixo
            if (base64.startsWith("data:")) {
                return base64;
            }
            return "data:image/png;base64," + base64;
        }
        
        // Alternativa: pode vir como "code" para QR Code text
        String code = json.path("code").asText("");
        if (!code.isEmpty()) {
            log.info("[EVOLUTION] QR Code text recebido, gerando imagem...");
            // Neste caso, seria necessário gerar a imagem do QR Code
            // Por enquanto, retornar o base64 se disponível
        }
        
        return base64;
    }

    /**
     * Desconecta a instância do WhatsApp
     */
    public boolean disconnect(Long tenantId) {
        if (!enabled) {
            return false;
        }

        String instanceName = getInstanceName(tenantId);
        
        try {
            String url = apiUrl + "/instance/logout/" + instanceName;
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            
            log.info("[EVOLUTION] Instância {} desconectada", instanceName);
            return true;
        } catch (Exception e) {
            log.error("[EVOLUTION] Erro ao desconectar: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envia mensagem de texto via WhatsApp
     */
    public boolean sendTextMessage(Long tenantId, String phoneNumber, String message) {
        if (!enabled) {
            log.info("[EVOLUTION][SIMULADO] Mensagem para {}: {}", phoneNumber, message);
            return true;
        }

        String instanceName = getInstanceName(tenantId);
        String normalizedPhone = normalizePhoneNumber(phoneNumber);
        
        try {
            String url = apiUrl + "/message/sendText/" + instanceName;
            
            Map<String, Object> body = new HashMap<>();
            body.put("number", normalizedPhone);
            body.put("text", message);
            
            // Opções adicionais
            Map<String, Object> options = new HashMap<>();
            options.put("delay", 1200); // delay entre mensagens para parecer mais humano
            body.put("options", options);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());
            restTemplate.postForEntity(url, entity, String.class);
            
            log.info("[EVOLUTION] Mensagem enviada para {} via instância {}", normalizedPhone, instanceName);
            return true;
            
        } catch (Exception e) {
            log.error("[EVOLUTION] Erro ao enviar mensagem para {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }

    /**
     * Normaliza número de telefone para formato internacional
     */
    private String normalizePhoneNumber(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        
        // Remover 0 inicial se houver
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        
        // Adicionar código do Brasil se não tiver
        if (digits.length() == 10 || digits.length() == 11) {
            digits = "55" + digits;
        }
        
        return digits;
    }

    /**
     * Formata número de telefone para exibição
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "";
        }
        
        String digits = phone.replaceAll("[^0-9]", "");
        
        // Remover código do país (55)
        if (digits.startsWith("55") && digits.length() > 11) {
            digits = digits.substring(2);
        }
        
        // Formatar como (XX) XXXXX-XXXX
        if (digits.length() == 11) {
            return "(" + digits.substring(0, 2) + ") " + digits.substring(2, 7) + "-" + digits.substring(7);
        } else if (digits.length() == 10) {
            return "(" + digits.substring(0, 2) + ") " + digits.substring(2, 6) + "-" + digits.substring(6);
        }
        
        return phone;
    }

    /**
     * Verifica se o serviço está habilitado
     */
    public boolean isEnabled() {
        return enabled;
    }
}

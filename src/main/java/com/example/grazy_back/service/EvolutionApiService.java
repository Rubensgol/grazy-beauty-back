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
            log.debug("[EVOLUTION] API não habilitada, retornando desconectado");
            return WhatsappStatusResponse.disconnected();
        }

        String instanceName = getInstanceName(tenantId);
        
        try {
            // Primeiro, tentar buscar a instância via fetchInstances
            String fetchUrl = apiUrl + "/instance/fetchInstances?instanceName=" + instanceName;
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            
            ResponseEntity<String> fetchResponse = restTemplate.exchange(fetchUrl, HttpMethod.GET, entity, String.class);
            JsonNode instances = objectMapper.readTree(fetchResponse.getBody());
            
            log.debug("[EVOLUTION] fetchInstances response: {}", fetchResponse.getBody());
            
            if (instances.isArray() && instances.size() > 0) {
                JsonNode instance = instances.get(0);
                
                // Verificar o estado da conexão - pode estar em diferentes caminhos
                String state = instance.path("instance").path("state").asText("");
                if (state.isEmpty()) {
                    state = instance.path("state").asText("");
                }
                if (state.isEmpty()) {
                    state = instance.path("instance").path("connectionStatus").asText("");
                }
                
                log.info("[EVOLUTION] Instância {} encontrada, estado: {}", instanceName, state);
                
                // Extrair número do telefone
                String phoneNumber = "";
                String owner = instance.path("instance").path("owner").asText("");
                if (owner.isEmpty()) {
                    owner = instance.path("owner").asText("");
                }
                if (!owner.isEmpty() && owner.contains("@")) {
                    phoneNumber = owner.split("@")[0];
                }
                
                // Verificar se está conectado
                if ("open".equalsIgnoreCase(state) || "connected".equalsIgnoreCase(state)) {
                    log.info("[EVOLUTION] Instância {} está CONECTADA, telefone: {}", instanceName, phoneNumber);
                    return WhatsappStatusResponse.connected(formatPhoneNumber(phoneNumber), instanceName);
                } else if ("connecting".equalsIgnoreCase(state)) {
                    log.info("[EVOLUTION] Instância {} está CONECTANDO", instanceName);
                    return new WhatsappStatusResponse("connecting");
                } else {
                    log.info("[EVOLUTION] Instância {} existe mas estado é: {}", instanceName, state);
                    // Instância existe mas não está conectada - tentar connectionState
                    return checkConnectionState(instanceName, entity);
                }
            } else {
                log.info("[EVOLUTION] Nenhuma instância encontrada para {}", instanceName);
                return WhatsappStatusResponse.disconnected();
            }
            
        } catch (HttpClientErrorException.NotFound e) {
            log.info("[EVOLUTION] Instância {} não existe ainda", instanceName);
            return WhatsappStatusResponse.disconnected();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[EVOLUTION] API não acessível em {}", apiUrl);
            return WhatsappStatusResponse.disconnected();
        } catch (Exception e) {
            log.error("[EVOLUTION] Erro ao verificar status: {}", e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || e.getMessage().contains("connect"))) {
                return WhatsappStatusResponse.disconnected();
            }
            return WhatsappStatusResponse.error("Erro ao verificar status: " + e.getMessage());
        }
    }
    
    /**
     * Verifica o estado da conexão via endpoint connectionState
     */
    private WhatsappStatusResponse checkConnectionState(String instanceName, HttpEntity<Void> entity) {
        try {
            String url = apiUrl + "/instance/connectionState/" + instanceName;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            
            log.debug("[EVOLUTION] connectionState response: {}", response.getBody());
            
            String state = json.path("state").asText("");
            if (state.isEmpty()) {
                state = json.path("instance").path("state").asText("close");
            }
            
            log.info("[EVOLUTION] connectionState para {}: {}", instanceName, state);
            
            if ("open".equalsIgnoreCase(state) || "connected".equalsIgnoreCase(state)) {
                return WhatsappStatusResponse.connected("", instanceName);
            }
            
            return WhatsappStatusResponse.disconnected();
        } catch (Exception e) {
            log.warn("[EVOLUTION] Erro ao verificar connectionState: {}", e.getMessage());
            return WhatsappStatusResponse.disconnected();
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

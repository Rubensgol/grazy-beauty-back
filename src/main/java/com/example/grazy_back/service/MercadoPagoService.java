package com.example.grazy_back.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.grazy_back.dto.PagamentoResponse;
import com.example.grazy_back.enums.StatusPagamentoEnum;
import com.example.grazy_back.model.Pagamento;
import com.example.grazy_back.model.Tenant;
import com.example.grazy_back.repository.PagamentoRepository;
import com.example.grazy_back.repository.TenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serviço para integração com Mercado Pago
 */
@Service
public class MercadoPagoService {
    
    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);
    
    private final PagamentoRepository pagamentoRepository;
    private final TenantRepository tenantRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${mercadopago.access.token:}")
    private String accessToken;
    
    @Value("${mercadopago.public.key:}")
    private String publicKey;
    
    @Value("${mercadopago.api.url:https://api.mercadopago.com}")
    private String apiUrl;
    
    @Value("${app.url:http://localhost:8080}")
    private String appUrl;
    
    public MercadoPagoService(
        PagamentoRepository pagamentoRepository,
        TenantRepository tenantRepository
    ) 
    {
        this.pagamentoRepository = pagamentoRepository;
        this.tenantRepository = tenantRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Verifica se o Mercado Pago está configurado
     */
    public boolean isConfigurado()
    {
        return accessToken != null && !accessToken.isEmpty();
    }
    
    /**
     * Cria uma preferência de pagamento no Mercado Pago
     */
    public PagamentoResponse criarPreferenciaPagamento(Pagamento pagamento) {
        try 
        {
            if (!isConfigurado())
                return PagamentoResponse.erro("Mercado Pago não configurado");
            
            Tenant tenant = tenantRepository.findById(pagamento.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant não encontrado"));
            
            // Monta o body da requisição
            Map<String, Object> preference = new HashMap<>();
            
            // Items
            Map<String, Object> item = new HashMap<>();
            item.put("title", String.format("Mensalidade %s - %02d/%d", 
                tenant.getNomeNegocio(), 
                pagamento.getMesReferencia(), 
                pagamento.getAnoReferencia()));
            item.put("quantity", 1);
            item.put("unit_price", pagamento.getValor().doubleValue());
            item.put("currency_id", "BRL");
            
            preference.put("items", new Object[]{item});
            
            // Payer (dados do tenant)
            Map<String, Object> payer = new HashMap<>();
            payer.put("name", tenant.getNomeAdmin());
            payer.put("email", tenant.getEmailAdmin());

            if (tenant.getTelefoneAdmin() != null) 
            {
                Map<String, Object> phone = new HashMap<>();
                phone.put("number", tenant.getTelefoneAdmin().replaceAll("[^0-9]", ""));
                payer.put("phone", phone);
            }

            preference.put("payer", payer);
            
            // Back URLs
            Map<String, String> backUrls = new HashMap<>();
            backUrls.put("success", appUrl + "/pagamento/sucesso");
            backUrls.put("failure", appUrl + "/pagamento/falha");
            backUrls.put("pending", appUrl + "/pagamento/pendente");
            preference.put("back_urls", backUrls);
            preference.put("auto_return", "approved");
            
            // Notification URL para webhook
            preference.put("notification_url", appUrl + "/api/pagamentos/webhook");
            
            // External reference para identificar o pagamento
            preference.put("external_reference", pagamento.getId().toString());
            
            // Payment methods
            Map<String, Object> paymentMethods = new HashMap<>();
            paymentMethods.put("installments", 1); // Apenas à vista
            preference.put("payment_methods", paymentMethods);
            
            // Faz a requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(preference, headers);
            
            String url = apiUrl + "/checkout/preferences";

            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            // Parse da resposta
            JsonNode responseBody = objectMapper.readTree(response.getBody());
            String preferenceId = responseBody.get("id").asText();
            String initPoint = responseBody.get("init_point").asText(); // Link para checkout
            
            // Atualiza o pagamento
            pagamento.setMercadoPagoPreferenceId(preferenceId);
            pagamento.setLinkPagamento(initPoint);
            pagamentoRepository.save(pagamento);
            
            log.info("[MERCADOPAGO] Preferência criada: {} para tenant {}", preferenceId, tenant.getId());
            
            return PagamentoResponse.sucesso(pagamento.getId(), initPoint, preferenceId);
            
        } 
        catch (Exception e) 
        {
            log.error("[MERCADOPAGO] Erro ao criar preferência: {}", e.getMessage(), e);
            return PagamentoResponse.erro("Erro ao criar pagamento: " + e.getMessage());
        }
    }
    
    /**
     * Processa notificação de webhook do Mercado Pago
     */
    public void processarWebhook(String type, String dataId) 
    {
        try 
        {
            if (!"payment".equals(type)) 
            {
                log.info("[MERCADOPAGO] Webhook ignorado, tipo: {}", type);
                return;
            }
            
            // Busca informações do pagamento
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String url = apiUrl + "/v1/payments/" + dataId;
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
            );
            
            JsonNode payment = objectMapper.readTree(response.getBody());
            
            String status = payment.get("status").asText();
            String externalReference = payment.has("external_reference") ? 
                payment.get("external_reference").asText() : null;
            
            if (externalReference == null) 
            {
                log.warn("[MERCADOPAGO] Webhook sem external_reference");
                return;
            }
            
            Long pagamentoId = Long.parseLong(externalReference);
            Pagamento pagamento = pagamentoRepository.findById(pagamentoId).orElse(null);
            
            if (pagamento == null) 
            {
                log.warn("[MERCADOPAGO] Pagamento não encontrado: {}", pagamentoId);
                return;
            }
            
            // Atualiza status
            pagamento.setMercadoPagoId(dataId);
            
            switch (status) 
            {
                case "approved":
                    pagamento.setStatus(StatusPagamentoEnum.APROVADO);
                    pagamento.setDataPagamento(LocalDateTime.now());
                    break;
                case "rejected":
                    pagamento.setStatus(StatusPagamentoEnum.REJEITADO);
                    break;
                case "cancelled":
                    pagamento.setStatus(StatusPagamentoEnum.CANCELADO);
                    break;
                case "refunded":
                    pagamento.setStatus(StatusPagamentoEnum.REEMBOLSADO);
                    break;
                case "in_process":
                    pagamento.setStatus(StatusPagamentoEnum.EM_PROCESSAMENTO);
                    break;
                default:
                    pagamento.setStatus(StatusPagamentoEnum.PENDENTE);
            }
            
            pagamentoRepository.save(pagamento);
            
            log.info("[MERCADOPAGO] Pagamento {} atualizado para status: {}", pagamentoId, status);
            
        }
        catch (Exception e) 
        {
            log.error("[MERCADOPAGO] Erro ao processar webhook: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Busca status de um pagamento no Mercado Pago
     */
    public String buscarStatusPagamento(String mercadoPagoId) 
    {
        try 
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String url = apiUrl + "/v1/payments/" + mercadoPagoId;
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
            );
            
            JsonNode payment = objectMapper.readTree(response.getBody());
            return payment.get("status").asText();
            
        } 
        catch (Exception e) 
        {
            log.error("[MERCADOPAGO] Erro ao buscar status: {}", e.getMessage());
            return "erro";
        }
    }
}

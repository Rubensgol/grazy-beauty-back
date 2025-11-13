package com.example.grazy_back.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.grazy_back.model.Agendamento;
import com.example.grazy_back.model.Cliente;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsappSenderService 
{
    private static final Logger log = LoggerFactory.getLogger(WhatsappSenderService.class);

    private final boolean enabled;
    private final String apiUrl;
    private final String token;
    private final RestTemplate restTemplate = new RestTemplate();

    private final MessageBuilderService messageBuilder;

    public WhatsappSenderService(
            @Value("${whatsapp.enabled:false}") boolean enabled,
            @Value("${whatsapp.api.url:https://graph.facebook.com/v19.0/WHATSAPP_PHONE_ID/messages}") String apiUrl,
            @Value("${whatsapp.api.token:}") String token,
            MessageBuilderService messageBuilder)
    {
        this.enabled = enabled;
        this.apiUrl = apiUrl;
        this.token = token;
        this.messageBuilder = messageBuilder;
    }

    public void enviar(Cliente usuario, Agendamento agendamento)
    {
        if (usuario == null || usuario.getTelefone() == null || usuario.getTelefone().isBlank()) 
        {
            log.warn("[WHATSAPP] Usuário sem telefone - agendamento {}", agendamento.getId());
            return;
        }

        String telefoneDestino = normalizarTelefone(usuario.getTelefone());
        String mensagem = messageBuilder.corpoLembreteAgendamentoTexto(usuario, agendamento);

        if (!enabled)
        {
            log.info("[WHATSAPP][SIMULADO] Envio para {} msg='{}'", telefoneDestino, mensagem);
            return;
        }

        try 
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("to", telefoneDestino);
            body.put("type", "text");
            Map<String, String> text = new HashMap<>();
            text.put("body", mensagem);
            body.put("text", text);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(apiUrl, entity, String.class);
            log.info("[WHATSAPP] Enviado para {} agendamento {}", telefoneDestino, agendamento.getId());
        }
        catch (Exception ex)
        {
            log.error("[WHATSAPP] Falha ao enviar para {} agendamento {}: {}", telefoneDestino, agendamento.getId(), ex.getMessage());
        }
    }


    private String normalizarTelefone(String telefone)
    {
        String digits = telefone.replaceAll("[^0-9]", "");

        if (digits.startsWith("0"))
            digits = digits.substring(1);

        return digits;
    }
}

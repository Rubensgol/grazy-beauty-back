package com.example.grazy_back.controller;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.dto.EmailRequest;
import com.example.grazy_back.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
@Tag(name = "E-mail", description = "Envio de e-mails")
public class EmailController
{
    private final EmailService emailService;

    public EmailController(EmailService emailService) 
    {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    @Operation(summary = "Envia e-mail", description = "Recebe um objeto com informações do e-mail e realiza o envio")
    public ResponseEntity<ApiResposta<Void>> send(@RequestBody EmailRequest req)
     {
        try 
        {
            emailService.send(req);
            return ResponseEntity.ok(ApiResposta.of(null));
        } 
        catch (IllegalArgumentException e) 
        {
            return ResponseEntity.badRequest().body(ApiResposta.error(e.getMessage()));
        }
         catch (MessagingException e) 
        {
            return ResponseEntity.internalServerError().body(ApiResposta.error("Falha ao enviar e-mail: " + e.getMessage()));
        }
    }
}

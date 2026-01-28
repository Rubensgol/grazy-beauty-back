package com.example.grazy_back.service;

import com.example.grazy_back.dto.EmailRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService 
{
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) 
    {
        this.mailSender = mailSender;
    }

    public void send(@NonNull EmailRequest req) throws MessagingException 
    {
        if (req.getTo() == null || req.getTo().isEmpty())
            throw new IllegalArgumentException("Lista de destinatários (to) é obrigatória");

        if (req.getSubject() == null)
            throw new IllegalArgumentException("Subject é obrigatório");

        if (req.getBody() == null)
            throw new IllegalArgumentException("Body é obrigatório");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        if (fromAddress != null && !fromAddress.isBlank())
            helper.setFrom(fromAddress);

        helper.setTo(req.getTo().toArray(String[]::new));

        if (req.getCc() != null && !req.getCc().isEmpty())
            helper.setCc(req.getCc().toArray(String[]::new));

        if (req.getBcc() != null && !req.getBcc().isEmpty())
            helper.setBcc(req.getBcc().toArray(String[]::new));

        helper.setSubject(req.getSubject());
        helper.setText(req.getBody(), req.isHtml());

        mailSender.send(message);
    }
    
    /**
     * Envia email de cobrança formatado
     */
    public void enviarEmailCobranca(
        String email, 
        String nomeCliente, 
        String nomeNegocio,
        BigDecimal valor,
        LocalDate dataVencimento,
        String linkPagamento
    ) throws MessagingException {
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dataFormatada = dataVencimento.format(formatter);
        
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .content { padding: 30px; }
                    .info-box { background-color: #f8f9fa; border-left: 4px solid #667eea; padding: 15px; margin: 20px 0; }
                    .info-box strong { display: block; margin-bottom: 5px; color: #333; }
                    .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; text-decoration: none; padding: 12px 30px; border-radius: 5px; margin: 20px 0; font-weight: bold; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>💰 Mensalidade Disponível</h1>
                    </div>
                    <div class="content">
                        <p>Olá <strong>%s</strong>,</p>
                        <p>Sua mensalidade do <strong>%s</strong> está disponível para pagamento.</p>
                        
                        <div class="info-box">
                            <strong>💵 Valor:</strong> R$ %.2f
                            <br><br>
                            <strong>📅 Vencimento:</strong> %s
                        </div>
                        
                        <p style="text-align: center;">
                            <a href="%s" class="button">Pagar Agora</a>
                        </p>
                        
                        <p style="font-size: 14px; color: #666;">
                            Clique no botão acima para ser redirecionado à página de pagamento segura do Mercado Pago.
                        </p>
                    </div>
                    <div class="footer">
                        <p>Este é um email automático. Em caso de dúvidas, entre em contato conosco.</p>
                        <p>© %d %s - Todos os direitos reservados</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            nomeCliente,
            nomeNegocio,
            valor,
            dataFormatada,
            linkPagamento,
            LocalDate.now().getYear(),
            nomeNegocio
        );
        
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(List.of(email));
        emailRequest.setSubject(String.format("Mensalidade %s - Pagamento Disponível", nomeNegocio));
        emailRequest.setBody(html);
        emailRequest.setHtml(true);
        
        send(emailRequest);
    }
}

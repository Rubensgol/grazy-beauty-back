package com.example.grazy_back.service;

import com.example.grazy_back.dto.EmailRequest;
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
}

package com.example.grazy_back.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.Data;

@Entity
@Data
public class ImageMetadata 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private Long size;
    private String sourceUrl;
    private Instant createdAt;
    
    // Indica se a imagem foi enviada para uso em um Serviço
    private Boolean forServico = false;
}

package com.example.grazy_back.model;

import java.time.Instant;

import jakarta.persistence.Column;
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
    
    @Column(name = "tenant_id")
    private Long tenantId;

    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private Long size;
    private String sourceUrl;
    private Instant createdAt;
    
    // Indica se a imagem foi enviada para uso em um Serviço
    private Boolean forServico = false;
    
    // Exibir na landing page (portfolio)
    private Boolean exibirLanding = false;
    
    // Ordem de exibição na landing
    private Integer ordemLanding = 0;
    
    // Título/descrição para exibição
    private String titulo;
    private String descricao;
    private String categoria;
}

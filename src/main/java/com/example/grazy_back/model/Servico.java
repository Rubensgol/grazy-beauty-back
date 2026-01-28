package com.example.grazy_back.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Servico
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id")
    private Long tenantId;
    
    private String nome;
    private String descricao;
    private Double preco;
    private Double custo; // Custo do serviço para cálculo de lucro
    private String imageStoredFilename;
    private Integer duracaoMinutos;
    private Integer ordem;
    private Boolean ativo = true;
    private Boolean exibirLanding = true; // Se o serviço aparece na landing page
}

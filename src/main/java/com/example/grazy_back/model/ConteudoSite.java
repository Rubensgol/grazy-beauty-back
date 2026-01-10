package com.example.grazy_back.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade para armazenar o conteúdo do site (landing page) de cada tenant.
 * Inclui seções Hero, About e outras configurações de conteúdo.
 */
@Entity
@Table(name = "conteudo_sites")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConteudoSite 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    // === HERO SECTION ===
    @Column(name = "hero_titulo")
    private String heroTitulo;

    @Column(name = "hero_subtitulo")
    private String heroSubtitulo;

    @Column(name = "hero_imagem_url")
    private String heroImagemUrl;

    // === ABOUT SECTION ===
    @Column(name = "about_titulo")
    private String aboutTitulo;

    @Column(name = "about_subtitulo")
    private String aboutSubtitulo;

    @Column(name = "about_texto", columnDefinition = "TEXT")
    private String aboutTexto;

    @Column(name = "about_imagem_url")
    private String aboutImagemUrl;

    // Stats do About (armazenado como JSON)
    @Column(name = "about_stats", columnDefinition = "TEXT")
    private String aboutStats;

    /**
     * Converte para o modelo Conteudo usado pela API.
     */
    public Conteudo toConteudo() 
    {
        Conteudo c = new Conteudo();
        
        ConteudoHero hero = new ConteudoHero();
        hero.setTitulo(this.heroTitulo);
        hero.setSubtitulo(this.heroSubtitulo);
        hero.setImagemUrl(this.heroImagemUrl);
        c.setHero(hero);
        
        ConteudoAbout about = new ConteudoAbout();
        about.setTitulo(this.aboutTitulo);
        about.setSubtitulo(this.aboutSubtitulo);
        about.setTexto(this.aboutTexto);
        about.setImagemUrl(this.aboutImagemUrl);
        
        // Parse stats JSON se existir
        if (this.aboutStats != null && !this.aboutStats.isEmpty()) 
        {
            try 
            {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.List<ConteudoStat> stats = mapper.readValue(
                    this.aboutStats, 
                    mapper.getTypeFactory().constructCollectionType(java.util.List.class, ConteudoStat.class)
                );
                about.setStats(stats);
            } 
            catch (Exception e) 
            {
                // Se falhar parse, deixa stats como null
            }
        }
        
        c.setAbout(about);
        return c;
    }

    /**
     * Cria ConteudoSite a partir do modelo Conteudo.
     */
    public static ConteudoSite fromConteudo(Conteudo conteudo, Long tenantId) 
    {
        ConteudoSiteBuilder builder = ConteudoSite.builder().tenantId(tenantId);
        
        if (conteudo.getHero() != null) 
        {
            builder.heroTitulo(conteudo.getHero().getTitulo())
                   .heroSubtitulo(conteudo.getHero().getSubtitulo())
                   .heroImagemUrl(conteudo.getHero().getImagemUrl());
        }
        
        if (conteudo.getAbout() != null) 
        {
            builder.aboutTitulo(conteudo.getAbout().getTitulo())
                   .aboutSubtitulo(conteudo.getAbout().getSubtitulo())
                   .aboutTexto(conteudo.getAbout().getTexto())
                   .aboutImagemUrl(conteudo.getAbout().getImagemUrl());
            
            // Serializa stats como JSON
            if (conteudo.getAbout().getStats() != null) 
            {
                try 
                {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    builder.aboutStats(mapper.writeValueAsString(conteudo.getAbout().getStats()));
                } 
                catch (Exception e) 
                {
                    // Se falhar serialização, deixa como null
                }
            }
        }
        
        return builder.build();
    }

    /**
     * Atualiza apenas os campos do Hero.
     */
    public void updateHero(ConteudoHero hero) 
    {
        if (hero.getTitulo() != null) this.heroTitulo = hero.getTitulo();
        if (hero.getSubtitulo() != null) this.heroSubtitulo = hero.getSubtitulo();
        if (hero.getImagemUrl() != null) this.heroImagemUrl = hero.getImagemUrl();
    }

    /**
     * Atualiza apenas os campos do About.
     */
    public void updateAbout(ConteudoAbout about) 
    {
        if (about.getTitulo() != null) this.aboutTitulo = about.getTitulo();
        if (about.getSubtitulo() != null) this.aboutSubtitulo = about.getSubtitulo();
        if (about.getTexto() != null) this.aboutTexto = about.getTexto();
        if (about.getImagemUrl() != null) this.aboutImagemUrl = about.getImagemUrl();
        
        if (about.getStats() != null) 
        {
            try 
            {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                this.aboutStats = mapper.writeValueAsString(about.getStats());
            } 
            catch (Exception e) 
            {
                // Se falhar serialização, ignora
            }
        }
    }
}

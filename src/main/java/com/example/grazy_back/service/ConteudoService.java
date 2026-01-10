package com.example.grazy_back.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.grazy_back.model.Conteudo;
import com.example.grazy_back.model.ConteudoAbout;
import com.example.grazy_back.model.ConteudoHero;
import com.example.grazy_back.model.ConteudoSite;
import com.example.grazy_back.repository.ConteudoSiteRepository;
import com.example.grazy_back.security.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service para gerenciar conteúdo do site (landing page).
 * Usa banco de dados com suporte a multi-tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConteudoService 
{
    private final ConteudoSiteRepository repository;

    /**
     * Obtém o conteúdo do tenant logado.
     */
    public Conteudo obter() 
    {
        Long tenantId = TenantContext.getCurrentTenantId();
        
        if (tenantId == null) 
        {
            log.debug("Nenhum tenant logado, retornando conteúdo padrão");
            return defaultConteudo();
        }
        
        return obterPorTenant(tenantId);
    }

    /**
     * Obtém conteúdo de um tenant específico (para landing page pública).
     */
    public Conteudo obterPorTenant(Long tenantId) 
    {
        return repository.findByTenantId(tenantId)
            .map(ConteudoSite::toConteudo)
            .orElse(defaultConteudo());
    }

    /**
     * Obtém conteúdo pelo subdomínio do tenant.
     */
    public Optional<Conteudo> obterPorSubdominio(String subdominio, TenantService tenantService) 
    {
        return tenantService.buscarTenantPorSubdominio(subdominio)
            .map(tenant -> obterPorTenant(tenant.getId()));
    }

    /**
     * Atualiza a seção Hero do tenant logado.
     */
    @Transactional
    public Conteudo atualizarHero(ConteudoHero hero) 
    {
        if (hero == null) 
        {
            throw new IllegalArgumentException("Hero inválido");
        }

        Long tenantId = TenantContext.requireTenantId();
        
        ConteudoSite site = repository.findByTenantId(tenantId)
            .orElseGet(() -> ConteudoSite.builder().tenantId(tenantId).build());
        
        site.updateHero(hero);
        repository.save(site);
        
        log.info("Hero atualizado para tenant {}", tenantId);
        return site.toConteudo();
    }

    /**
     * Atualiza a seção About do tenant logado.
     */
    @Transactional
    public Conteudo atualizarAbout(ConteudoAbout about) 
    {
        if (about == null) 
        {
            throw new IllegalArgumentException("About inválido");
        }

        Long tenantId = TenantContext.requireTenantId();
        
        ConteudoSite site = repository.findByTenantId(tenantId)
            .orElseGet(() -> ConteudoSite.builder().tenantId(tenantId).build());
        
        site.updateAbout(about);
        repository.save(site);
        
        log.info("About atualizado para tenant {}", tenantId);
        return site.toConteudo();
    }

    /**
     * Salva conteúdo completo para um tenant.
     */
    @Transactional
    public Conteudo salvarConteudo(Conteudo conteudo) 
    {
        Long tenantId = TenantContext.requireTenantId();
        return salvarConteudoPorTenant(tenantId, conteudo);
    }

    /**
     * Salva conteúdo completo para um tenant específico.
     */
    @Transactional
    public Conteudo salvarConteudoPorTenant(Long tenantId, Conteudo conteudo) 
    {
        ConteudoSite site = repository.findByTenantId(tenantId)
            .orElseGet(() -> ConteudoSite.builder().tenantId(tenantId).build());
        
        if (conteudo.getHero() != null) 
        {
            site.updateHero(conteudo.getHero());
        }
        
        if (conteudo.getAbout() != null) 
        {
            site.updateAbout(conteudo.getAbout());
        }
        
        repository.save(site);
        log.info("Conteúdo completo salvo para tenant {}", tenantId);
        
        return site.toConteudo();
    }

    /**
     * Retorna conteúdo padrão quando não há configuração específica.
     */
    private Conteudo defaultConteudo() 
    {
        Conteudo c = new Conteudo();
        
        ConteudoHero h = new ConteudoHero();
        h.setTitulo("Bem-vindo(a)");
        h.setSubtitulo("Seu salão de beleza");
        h.setImagemUrl("");
        
        ConteudoAbout a = new ConteudoAbout();
        a.setTitulo("Sobre nós");
        a.setSubtitulo("");
        a.setTexto("");
        a.setImagemUrl("");
        
        c.setHero(h);
        c.setAbout(a);
        
        return c;
    }
}

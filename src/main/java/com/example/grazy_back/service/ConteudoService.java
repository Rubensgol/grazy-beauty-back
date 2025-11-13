package com.example.grazy_back.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.grazy_back.model.Conteudo;
import com.example.grazy_back.model.ConteudoAbout;
import com.example.grazy_back.model.ConteudoHero;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class ConteudoService 
{
    private final Path filePath;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Conteudo atual;

    public ConteudoService(@Value("${conteudo.config.file:content-config.json}") String file) 
    {
        this.filePath = Paths.get(file).toAbsolutePath();
    }

    @PostConstruct
    void init() 
    {
        lock.writeLock().lock();
        try 
        {
            if (Files.exists(filePath)) 
            {
                try 
                {
                    atual = mapper.readValue(Files.readAllBytes(filePath), Conteudo.class);
                } 
                catch (IOException e) 
                {
                    System.err.println("Failed to read content config from " + filePath + ": " + e.getMessage());
                    atual = defaultConteudo();
                }
            } 
            else
             {
                atual = defaultConteudo();
            
                try
                {
                    salvar(atual);
                }
                catch (IOException e) 
                {
                    System.err.println("Failed to save default content config to " + filePath + ": " + e.getMessage());
                }
            }
        } 
        finally 
        {
            lock.writeLock().unlock();
        }
    }

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

    public Conteudo obter()
     {
        lock.readLock().lock();
        
        try { return atual; }
        
        finally { lock.readLock().unlock(); }
    }

    public Conteudo atualizarHero(ConteudoHero hero) throws IOException 
    {
        if (hero == null) 
            throw new IllegalArgumentException("Hero inválido");
        
        lock.writeLock().lock();
        
        try
        {
            if (atual.getHero() == null) atual.setHero(new ConteudoHero());
            if (hero.getTitulo() != null) atual.getHero().setTitulo(hero.getTitulo());
            if (hero.getSubtitulo() != null) atual.getHero().setSubtitulo(hero.getSubtitulo());
            if (hero.getImagemUrl() != null) atual.getHero().setImagemUrl(hero.getImagemUrl());
            salvar(atual);
            return atual;
        } 
        finally { lock.writeLock().unlock(); }
    }

    public Conteudo atualizarAbout(ConteudoAbout about) throws IOException 
    {
        if (about == null) throw new IllegalArgumentException("About inválido");
        
        lock.writeLock().lock();
        
        try 
        {
            ConteudoAbout dst = atual.getAbout();
            if (dst == null) { dst = new ConteudoAbout(); atual.setAbout(dst); }
            if (about.getTitulo() != null) dst.setTitulo(about.getTitulo());
            if (about.getSubtitulo() != null) dst.setSubtitulo(about.getSubtitulo());
            if (about.getTexto() != null) dst.setTexto(about.getTexto());
            if (about.getImagemUrl() != null) dst.setImagemUrl(about.getImagemUrl());
            if (about.getStats() != null) dst.setStats(about.getStats());
            salvar(atual);
            return atual;
        } 
        finally 
        { 
            lock.writeLock().unlock();
        }
    }

    private void salvar(Conteudo c) throws IOException
    {
        if (filePath.getParent() != null) Files.createDirectories(filePath.getParent());
        
        Path tmp = Files.createTempFile(filePath.getParent() != null ? filePath.getParent() : Paths.get("."), "conteudo-", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), c);
        
        Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}

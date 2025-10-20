package com.example.grazy_back.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.grazy_back.model.ConfiguracaoNotificacao;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class ConfiguracaoNotificacaoService 
{
    private final Path filePath;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ConfiguracaoNotificacao atual;

    public ConfiguracaoNotificacaoService(@Value("${notificacao.config.file:notification-config.json}") String file) 
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
                    atual = mapper.readValue(Files.readAllBytes(filePath), ConfiguracaoNotificacao.class);
                } 
                catch (IOException e) 
                {
                    System.err.println("Failed to read notification config from " + filePath + ": " + e.getMessage());
                    atual = defaultConfig();
                }
            } 
            else 
            {
                atual = defaultConfig();
                try 
                {
                    salvar(atual);
                } 
                catch (IOException e) 
                {
                    System.err.println("Failed to save default notification config to " + filePath + ": " + e.getMessage());
                }
            }
        }
        finally 
        {
            lock.writeLock().unlock();
        }
    }

    private ConfiguracaoNotificacao defaultConfig() 
    {
        ConfiguracaoNotificacao c = new ConfiguracaoNotificacao();
        c.setAtivo(false);
        c.setPeriodoMinutos(30L);
        c.setPlataformas(Set.of("EMAIL"));
        return c;
    }

    public ConfiguracaoNotificacao obter() 
    {
        lock.readLock().lock();
        
        try
        {
            return atual;
        } 
        finally 
        {
            lock.readLock().unlock();
        }
    }

    public ConfiguracaoNotificacao atualizar(ConfiguracaoNotificacao nova) throws IOException 
    {
        if (nova.getPeriodoMinutos() == null || nova.getPeriodoMinutos() <= 0)
            throw new IllegalArgumentException("PeriodoMinutos deve ser > 0");

        lock.writeLock().lock();
        
        try 
        {
            this.atual = nova;
            salvar(nova);
            return this.atual;
        }
        finally 
        {
            lock.writeLock().unlock();
        }
    }

    private void salvar(ConfiguracaoNotificacao c) throws IOException 
    {
        if (filePath.getParent() != null) 
            Files.createDirectories(filePath.getParent());

        Path tmp = Files.createTempFile(filePath.getParent() != null ? filePath.getParent() : Paths.get("."), "cfg-", ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), c);
        Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}

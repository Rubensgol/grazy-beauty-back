package com.example.grazy_back.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.grazy_back.service.TenantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobs 
{
    private final TenantService tenantService;

    /**
     * Reseta os contadores de agendamentos mensais de todos os tenants.
     * Executado no primeiro dia de cada mês às 00:01.
     */
    @Scheduled(cron = "0 1 0 1 * *")
    public void resetarContadoresMensais() 
    {
        log.info("Executando job de reset de contadores mensais...");
        try 
        {
            tenantService.resetarContadoresMensais();
            log.info("Contadores mensais resetados com sucesso");
        } 
        catch (Exception e) 
        {
            log.error("Erro ao resetar contadores mensais: {}", e.getMessage(), e);
        }
    }
}

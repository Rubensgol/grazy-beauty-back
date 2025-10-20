package com.example.grazy_back.service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.grazy_back.model.ConfiguracaoNotificacao;
import com.example.grazy_back.model.Agendamento;
import com.example.grazy_back.enums.StatusAgendamentoEnum;
import com.example.grazy_back.repository.AgendamentoRepository;

@Service
public class NotificacaoAgendadaService 
{
    private static final Logger log = LoggerFactory.getLogger(NotificacaoAgendadaService.class);

    private final ConfiguracaoNotificacaoService configService;
    private final AtomicLong ultimaExecucaoEpochMillis = new AtomicLong(0L);
    private final AgendamentoRepository agendamentoRepository;
    private final WhatsappSenderService whatsappSenderService;

    public NotificacaoAgendadaService(ConfiguracaoNotificacaoService configService,
                                      AgendamentoRepository agendamentoRepository,
                                      WhatsappSenderService whatsappSenderService)
    {
        this.configService = configService;
        this.agendamentoRepository = agendamentoRepository;
        this.whatsappSenderService = whatsappSenderService;
    }

    // Verifica a cada 1 minuto se já passou o período configurado para disparar notificações
    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void verificar() 
    {
        ConfiguracaoNotificacao cfg = configService.obter();

        if (!cfg.isAtivo())
            return; // silencioso se desativado

        Long periodoMin = cfg.getPeriodoMinutos();

        if (periodoMin == null || periodoMin <= 0)
        {
            log.warn("[NOTIFICACAO] periodoMinutos inválido: {}", periodoMin);
            return;
        }

        long agora = System.currentTimeMillis();
        long anterior = ultimaExecucaoEpochMillis.get();
        long intervaloMillis = periodoMin * 60_000L;

        if (agora - anterior < intervaloMillis) 
            return; // ainda não chegou o tempo

        if (ultimaExecucaoEpochMillis.compareAndSet(anterior, agora))
            executarVerificacao(cfg);
    }

    private void executarVerificacao(ConfiguracaoNotificacao cfg)
    {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limite = agora.plusMinutes(cfg.getPeriodoMinutos());

        List<Agendamento> proximos = agendamentoRepository
            .findByStatusAndNotificadoFalseAndDataHoraBetweenOrderByDataHoraAsc(StatusAgendamentoEnum.PENDENTE, agora, limite);

        if (proximos.isEmpty())
        {
            log.debug("[NOTIFICACAO] Nenhum agendamento nos próximos {} min", cfg.getPeriodoMinutos());
            return;
        }

        Set<String> plataformas = cfg.getPlataformas();

        if (plataformas == null || plataformas.isEmpty())
        {
            log.info("[NOTIFICACAO] Plataformas vazias - não notificando {} agendamentos", proximos.size());
            return;
        }

        for (Agendamento a : proximos) 
        {
            for (String p : plataformas)
            {
                log.info("[NOTIFICACAO] Plataforma={} AgendamentoID={} Usuario={} Servico={} DataHora={} (dentro de ~{} min)",
                        p,
                        a.getId(),
                        a.getUsuario() != null ? a.getUsuario().getNome() : "-",
                        a.getServico() != null ? a.getServico().getNome() : "-",
                        a.getDataHora(),
                        cfg.getPeriodoMinutos());
                if ("WHATSAPP".equalsIgnoreCase(p)) 
                    whatsappSenderService.enviar(a.getUsuario(), a);
            }

            a.setNotificado(true);
            a.setNotificadoEm(java.time.Instant.now());
        }

        agendamentoRepository.saveAll(proximos);
    }
}

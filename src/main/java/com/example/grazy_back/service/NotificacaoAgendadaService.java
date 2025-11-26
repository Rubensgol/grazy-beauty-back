package com.example.grazy_back.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.grazy_back.model.ConfiguracaoNotificacao;
import com.example.grazy_back.model.Agendamento;
import com.example.grazy_back.dto.EmailRequest;
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
    private final EmailService emailService;
    private final MessageBuilderService messageBuilder;

    public NotificacaoAgendadaService(ConfiguracaoNotificacaoService configService,
                                      AgendamentoRepository agendamentoRepository,
                                      WhatsappSenderService whatsappSenderService,
                                      EmailService emailService,
                                      MessageBuilderService messageBuilder)
    {
        this.configService = configService;
        this.agendamentoRepository = agendamentoRepository;
        this.whatsappSenderService = whatsappSenderService;
        this.emailService = emailService;
        this.messageBuilder = messageBuilder;
    }

    // Verifica a cada 1 minuto se já passou o período configurado para disparar notificações
    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void verificar() 
    {
        ConfiguracaoNotificacao cfg = configService.obter();

        log.info("Verificando notificacoes");

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

        Map<String, String> plataformas = cfg.getPlataformas();

        if (plataformas == null || plataformas.isEmpty())
        {
            log.info("[NOTIFICACAO] Plataformas vazias - não notificando {} agendamentos");
            return;
        }

        if (ultimaExecucaoEpochMillis.compareAndSet(anterior, agora))
            executarVerificacao(cfg, plataformas);
    }

    private void executarVerificacao(ConfiguracaoNotificacao cfg, Map<String, String> plataformas)
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

        for (Agendamento a : proximos) 
        {
            for (String p : plataformas.keySet())
            {
                log.info("[NOTIFICACAO] Plataforma={} AgendamentoID={} Usuario={} Servico={} DataHora={} (dentro de ~{} min)",
                        p,
                        a.getId(),
                        a.getUsuario() != null ? a.getUsuario().getNome() : "-",
                        a.getServico() != null ? a.getServico().getNome() : "-",
                        a.getDataHora(),
                        cfg.getPeriodoMinutos());

                if ("WHATSAPP".equalsIgnoreCase(p)) 
                {
                    whatsappSenderService.enviar(a.getUsuario(), a);
                }
                else if ("EMAIL".equalsIgnoreCase(p))
                {
                    try
                    {
                        var destinatarioCliente = a.getUsuario() != null ? a.getUsuario().getEmail() : null;
                        var fallback = plataformas.get(p);

                        String to = (destinatarioCliente != null && !destinatarioCliente.isBlank()) ? destinatarioCliente : fallback;

                        if (to == null || to.isBlank())
                        {
                            log.warn("[EMAIL] Sem destinatário para agendamento {} (cliente/fallback vazios)", a.getId());
                        }
                        else
                        {
                            EmailRequest er = new EmailRequest();
                            er.setTo(List.of(to));
                            er.setSubject(messageBuilder.assuntoLembreteAgendamento(a));
                            er.setBody(messageBuilder.corpoLembreteAgendamentoTexto(a.getUsuario(), a));
                            er.setHtml(false);
                            emailService.send(er);
                            log.info("[EMAIL] Enviado lembrete para {} agendamento {}", to, a.getId());
                        }
                    }
                    catch (Exception ex)
                    {
                        log.error("[EMAIL] Falha ao enviar lembrete do agendamento {}: {}", a.getId(), ex.getMessage());
                    }
                }
            }

            a.setNotificado(true);
            a.setNotificadoEm(Instant.now());
        }

        agendamentoRepository.saveAll(proximos);
    }

    // Resumo diário dos agendamentos do dia (07:00). Pode ser ajustado depois via config.
    @Scheduled(cron = "0 0 7 * * *")
    public void enviarResumoDiario()
    {
        ConfiguracaoNotificacao cfg = configService.obter();
        if (cfg == null || !cfg.isResumoAtivo())
            return;

        String destino = (cfg.getResumoEmail() != null && !cfg.getResumoEmail().isBlank())
                ? cfg.getResumoEmail()
                : (cfg.getPlataformas() != null ? cfg.getPlataformas().get("EMAIL") : null);

        if (destino == null || destino.isBlank())
        {
            log.warn("[RESUMO] resumoAtivo=true mas nenhum email destino configurado (resumoEmail ou plataformas.EMAIL)");
            return;
        }

        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio = hoje.atStartOfDay();
        LocalDateTime fim = inicio.plusDays(1);
        List<Agendamento> doDia = agendamentoRepository.findByDataHoraBetweenOrderByDataHoraAsc(inicio, fim);

        try
        {
            EmailRequest req = new EmailRequest();
            req.setTo(List.of(destino));
            req.setSubject(messageBuilder.assuntoResumoAgendamentos(hoje));
            req.setBody(messageBuilder.corpoResumoAgendamentos(doDia, hoje));
            req.setHtml(false); // texto simples; pode evoluir para HTML
            emailService.send(req);
            log.info("[RESUMO] Enviado resumo diário para {} com {} agendamentos", destino, doDia.size());
        }
        catch (Exception ex)
        {
            log.error("[RESUMO] Falha ao enviar resumo diário para {}: {}", destino, ex.getMessage());
        }
    }
}

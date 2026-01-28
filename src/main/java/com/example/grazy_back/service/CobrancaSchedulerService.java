package com.example.grazy_back.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.grazy_back.dto.PagamentoResponse;
import com.example.grazy_back.enums.PlanoEnum;
import com.example.grazy_back.enums.StatusPagamentoEnum;
import com.example.grazy_back.enums.StatusTenantEnum;
import com.example.grazy_back.model.Pagamento;
import com.example.grazy_back.model.Tenant;
import com.example.grazy_back.repository.PagamentoRepository;
import com.example.grazy_back.repository.TenantRepository;

/**
 * Serviço de agendamento para envio automático de cobranças
 */
@Service
public class CobrancaSchedulerService {
    
    private static final Logger log = LoggerFactory.getLogger(CobrancaSchedulerService.class);
    
    // Valores de planos (pode ser configurável no futuro)
    private static final BigDecimal VALOR_BASICO = new BigDecimal("29.90");
    private static final BigDecimal VALOR_PRO = new BigDecimal("59.90");
    private static final BigDecimal VALOR_ENTERPRISE = new BigDecimal("99.90");
    
    private final TenantRepository tenantRepository;
    private final PagamentoRepository pagamentoRepository;
    private final MercadoPagoService mercadoPagoService;
    private final WhatsappSenderService whatsappSenderService;
    private final EmailService emailService;
    
    public CobrancaSchedulerService(
        TenantRepository tenantRepository,
        PagamentoRepository pagamentoRepository,
        MercadoPagoService mercadoPagoService,
        WhatsappSenderService whatsappSenderService,
        EmailService emailService
    ) {
        this.tenantRepository = tenantRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.whatsappSenderService = whatsappSenderService;
        this.emailService = emailService;
    }
    
    /**
     * Roda todos os dias às 8h da manhã para verificar cobranças
     * Cron: segundos minutos horas dia mês dia-semana
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void processarCobrancasDiarias() {
        log.info("[SCHEDULER] Iniciando processamento de cobranças diárias");
        
        LocalDate hoje = LocalDate.now();
        int diaAtual = hoje.getDayOfMonth();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();
        
        // Busca todos os tenants ativos com dia de pagamento configurado
        List<Tenant> tenants = tenantRepository.findByStatusAndAtivoTrue(StatusTenantEnum.ATIVO);
        
        int processados = 0;
        int enviados = 0;
        
        for (Tenant tenant : tenants) {
            try {
                // Verifica se tem dia de pagamento configurado
                if (tenant.getDiaPagamento() == null) {
                    continue;
                }
                
                // Verifica se hoje é o dia de pagamento
                if (tenant.getDiaPagamento() != diaAtual) {
                    continue;
                }
                
                processados++;
                
                // Verifica se já existe pagamento para este mês
                var pagamentoExistente = pagamentoRepository.findByTenantIdAndMesReferenciaAndAnoReferencia(
                    tenant.getId(), 
                    mesAtual, 
                    anoAtual
                );
                
                if (pagamentoExistente.isPresent()) {
                    Pagamento pag = pagamentoExistente.get();
                    
                    // Se já foi enviado, pula
                    if (pag.getEnviadoWhatsapp() && pag.getEnviadoEmail()) {
                        continue;
                    }
                    
                    // Se tem link mas não enviou, reenvia
                    if (pag.getLinkPagamento() != null) {
                        enviarNotificacoesCobranca(tenant, pag);
                        enviados++;
                        continue;
                    }
                }
                
                // Cria novo pagamento
                BigDecimal valor = obterValorPlano(tenant.getPlano());
                
                Pagamento pagamento = new Pagamento();
                pagamento.setTenantId(tenant.getId());
                pagamento.setValor(valor);
                pagamento.setMesReferencia(mesAtual);
                pagamento.setAnoReferencia(anoAtual);
                pagamento.setStatus(StatusPagamentoEnum.PENDENTE);
                pagamento.setDataVencimento(hoje.plusDays(5).atStartOfDay()); // Vencimento em 5 dias
                
                pagamento = pagamentoRepository.save(pagamento);
                
                // Cria preferência no Mercado Pago
                PagamentoResponse response = mercadoPagoService.criarPreferenciaPagamento(pagamento);
                
                if (!response.getSucesso()) {
                    log.error("[SCHEDULER] Erro ao criar pagamento MP para tenant {}: {}", 
                        tenant.getId(), response.getMensagem());
                    continue;
                }
                
                // Envia notificações
                enviarNotificacoesCobranca(tenant, pagamento);
                enviados++;
                
                log.info("[SCHEDULER] Cobrança criada e enviada para tenant {}", tenant.getId());
                
            } catch (Exception e) {
                log.error("[SCHEDULER] Erro ao processar cobrança para tenant {}: {}", 
                    tenant.getId(), e.getMessage(), e);
            }
        }
        
        log.info("[SCHEDULER] Processamento concluído. Tenants processados: {}, Cobranças enviadas: {}", 
            processados, enviados);
    }
    
    /**
     * Envia as notificações de cobrança via WhatsApp e Email
     */
    private void enviarNotificacoesCobranca(Tenant tenant, Pagamento pagamento) {
        boolean enviadoWhatsapp = false;
        boolean enviadoEmail = false;
        
        String mensagem = montarMensagemCobranca(tenant, pagamento);
        
        // Envia WhatsApp se configurado
        if (Boolean.TRUE.equals(tenant.getEnviarCobrancaWhatsapp()) && tenant.getTelefoneAdmin() != null) {
            try {
                whatsappSenderService.enviarMensagemCobranca(
                    tenant.getTelefoneAdmin(), 
                    mensagem
                );
                enviadoWhatsapp = true;
                log.info("[SCHEDULER] WhatsApp enviado para tenant {}", tenant.getId());
            } catch (Exception e) {
                log.error("[SCHEDULER] Erro ao enviar WhatsApp para tenant {}: {}", 
                    tenant.getId(), e.getMessage());
            }
        }
        
        // Envia Email se configurado
        if (Boolean.TRUE.equals(tenant.getEnviarCobrancaEmail())) {
            try {
                emailService.enviarEmailCobranca(
                    tenant.getEmailAdmin(),
                    tenant.getNomeAdmin() != null ? tenant.getNomeAdmin() : "Cliente",
                    tenant.getNomeNegocio(),
                    pagamento.getValor(),
                    pagamento.getDataVencimento().toLocalDate(),
                    pagamento.getLinkPagamento()
                );
                enviadoEmail = true;
                log.info("[SCHEDULER] Email enviado para tenant {}", tenant.getId());
            } catch (Exception e) {
                log.error("[SCHEDULER] Erro ao enviar email para tenant {}: {}", 
                    tenant.getId(), e.getMessage());
            }
        }
        
        // Atualiza flags de envio
        if (enviadoWhatsapp || enviadoEmail) {
            pagamento.setEnviadoWhatsapp(enviadoWhatsapp);
            pagamento.setEnviadoEmail(enviadoEmail);
            pagamento.setDataEnvioCobranca(LocalDateTime.now());
            pagamentoRepository.save(pagamento);
        }
    }
    
    /**
     * Monta a mensagem de cobrança
     */
    private String montarMensagemCobranca(Tenant tenant, Pagamento pagamento) {
        String nome = tenant.getNomeAdmin() != null ? tenant.getNomeAdmin() : "Cliente";
        
        return String.format(
            "Olá %s! 👋\n\n" +
            "Sua mensalidade do *%s* referente a *%02d/%d* está disponível.\n\n" +
            "💰 *Valor:* R$ %.2f\n" +
            "📅 *Vencimento:* %02d/%02d/%d\n\n" +
            "Clique no link abaixo para pagar:\n%s\n\n" +
            "Em caso de dúvidas, estamos à disposição! 😊",
            nome,
            tenant.getNomeNegocio(),
            pagamento.getMesReferencia(),
            pagamento.getAnoReferencia(),
            pagamento.getValor(),
            pagamento.getDataVencimento().getDayOfMonth(),
            pagamento.getDataVencimento().getMonthValue(),
            pagamento.getDataVencimento().getYear(),
            pagamento.getLinkPagamento()
        );
    }
    
    /**
     * Obtém o valor do plano
     */
    private BigDecimal obterValorPlano(PlanoEnum plano) {
        if (plano == null) return VALOR_BASICO;
        return switch (plano) {
            case BASICO -> VALOR_BASICO;
            case PRO -> VALOR_PRO;
            case ENTERPRISE -> VALOR_ENTERPRISE;
            default -> VALOR_BASICO;
        };
    }
}

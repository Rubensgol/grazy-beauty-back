package com.example.grazy_back.service;

import java.util.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.grazy_back.DTO.AgendamentoRequest;
import com.example.grazy_back.enums.StatusAgendamentoEnum;
import com.example.grazy_back.model.Agendamento;
import com.example.grazy_back.model.TransacaoFinanceira;
import com.example.grazy_back.model.Servico;
import com.example.grazy_back.model.Cliente;
import com.example.grazy_back.repository.AgendamentoRepository;
import com.example.grazy_back.repository.ServicoRepository;
import com.example.grazy_back.repository.ClienteRepository;
import com.example.grazy_back.repository.TransacaoFinanceiraRepository;
import com.example.grazy_back.enums.TipoTransacaoEnum;
import com.example.grazy_back.security.TenantContext;

@Service
public class AgendamentoService 
{
    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    
    private final AgendamentoRepository agendamentoRepository;
    private final ServicoRepository servicoRepository;
    private final ClienteRepository usuarioRepository;
    private final TransacaoFinanceiraRepository transacaoRepository;
    private final EvolutionApiService evolutionApiService;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              ServicoRepository servicoRepository,
                              ClienteRepository usuarioRepository,
                              TransacaoFinanceiraRepository transacaoRepository,
                              EvolutionApiService evolutionApiService)
    {
        this.agendamentoRepository = agendamentoRepository;
        this.servicoRepository = servicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.transacaoRepository = transacaoRepository;
        this.evolutionApiService = evolutionApiService;
    }

    public Optional<Agendamento> criar(AgendamentoRequest req)
    {
        if (req.getServicoId() == null || req.getUsuarioId() == null || req.getDataHora() == null) return Optional.empty();
        Optional<Servico> servicoOpt = servicoRepository.findById(req.getServicoId());
        Optional<Cliente> usuarioOpt = usuarioRepository.findById(req.getUsuarioId());
        if (servicoOpt.isEmpty() || usuarioOpt.isEmpty()) return Optional.empty();

        LocalDateTime dataHora;

        try 
        {
            dataHora = LocalDateTime.parse(req.getDataHora());
        } 
        catch (DateTimeParseException ex)
        {
            return Optional.empty();
        }

        Agendamento a = new Agendamento();
        a.setTenantId(TenantContext.getCurrentTenantId());
        a.setServico(servicoOpt.get());
        a.setUsuario(usuarioOpt.get());
        a.setDataHora(dataHora);
        a.setObs(req.getObs());

        Agendamento salvo = agendamentoRepository.save(a);
        
        // Enviar notificação WhatsApp de confirmação
        enviarNotificacaoAgendamentoCriado(salvo);

        return Optional.of(salvo);
    }
    
    /**
     * Envia notificação via WhatsApp quando agendamento é criado
     */
    private void enviarNotificacaoAgendamentoCriado(Agendamento agendamento) {
        try {
            Cliente cliente = agendamento.getUsuario();
            Servico servico = agendamento.getServico();
            
            if (cliente == null || cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
                log.debug("[WHATSAPP] Cliente sem telefone - agendamento {}", agendamento.getId());
                return;
            }
            
            String dataFormatada = agendamento.getDataHora().format(DATE_FORMATTER);
            String mensagem = String.format(
                "✅ *Agendamento Confirmado!*\n\n" +
                "Olá %s! Seu horário foi agendado com sucesso.\n\n" +
                "📋 *Serviço:* %s\n" +
                "📅 *Data:* %s\n" +
                "%s\n\n" +
                "Aguardamos você! 💜",
                cliente.getNome(),
                servico != null ? servico.getNome() : "Não especificado",
                dataFormatada,
                servico != null && servico.getPreco() != null 
                    ? "💰 *Valor:* R$ " + String.format("%.2f", servico.getPreco())
                    : ""
            );
            
            evolutionApiService.sendTextMessage(
                agendamento.getTenantId(),
                cliente.getTelefone(),
                mensagem
            );
            
            log.info("[WHATSAPP] Notificação de agendamento enviada para {}", cliente.getTelefone());
            
        } catch (Exception e) {
            log.error("[WHATSAPP] Erro ao enviar notificação de agendamento: {}", e.getMessage());
            // Não falha o agendamento se a notificação falhar
        }
    }

    public List<Agendamento> listar() 
    {
        Long tenantId = TenantContext.getCurrentTenantId();
        
        if (tenantId == null && TenantContext.isSuperAdmin()) 
        {
            return agendamentoRepository.findAll();
        }
        
        return agendamentoRepository.findByTenantId(tenantId);
    }

    public List<Agendamento> listarPorDia(LocalDateTime inicioDia)
    {
        Long tenantId = TenantContext.getCurrentTenantId();
        LocalDateTime fimDia = inicioDia.plusDays(1);
        
        if (tenantId == null && TenantContext.isSuperAdmin()) 
        {
            return agendamentoRepository.findByDataHoraBetweenOrderByDataHoraAsc(inicioDia, fimDia);
        }
        
        return agendamentoRepository.findByTenantIdAndDataHoraBetweenOrderByDataHoraAsc(tenantId, inicioDia, fimDia);
    }

    public long contarNoMes(int ano, int mes)
    {
        Long tenantId = TenantContext.getCurrentTenantId();
        LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0, 0);
        LocalDateTime fim = inicio.plusMonths(1);
        
        if (tenantId == null && TenantContext.isSuperAdmin()) 
        {
            return agendamentoRepository.countByDataHoraBetween(inicio, fim);
        }
        
        return agendamentoRepository.countByTenantIdAndDataHoraBetween(tenantId, inicio, fim);
    }

    public Optional<Agendamento> finalizar(Long id) 
    {
        return agendamentoRepository.findById(id).map(a -> {
            if (a.getStatus() == StatusAgendamentoEnum.FINALIZADO) return a; // já finalizado

            a.setStatus(StatusAgendamentoEnum.FINALIZADO);
            a.setFinalizedAt(Instant.now());
            Agendamento salvo = agendamentoRepository.save(a);

            // Cria transação financeira de receita (valor do serviço) usando nome do cliente
            Servico servico = a.getServico();
            Cliente usuario = a.getUsuario();

            if (servico != null && usuario != null && servico.getPreco() != null) 
            {
                TransacaoFinanceira t = new TransacaoFinanceira();
                t.setTenantId(a.getTenantId());
                t.setValor(servico.getPreco());
                t.setDescricao("Serviço: " + servico.getNome() + " - Cliente: " + usuario.getNome());
                t.setTipo(TipoTransacaoEnum.RECEITA);
                t.setData(Date.from(Instant.now().atZone(ZoneId.systemDefault()).toInstant()));
                transacaoRepository.save(t);
            }

            return salvo;
        });
    }

    public Optional<Agendamento> cancelar(Long id, String motivo)
    {
        return agendamentoRepository.findById(id).flatMap(a -> {
            if (a.getStatus() == StatusAgendamentoEnum.FINALIZADO) return Optional.empty();

            if (a.getStatus() == StatusAgendamentoEnum.CANCELADO) return Optional.of(a);

            a.setStatus(StatusAgendamentoEnum.CANCELADO);
            a.setCanceledAt(Instant.now());

            if (motivo != null && !motivo.isBlank())
                a.setCancelReason(motivo);

            Agendamento salvo = agendamentoRepository.save(a);
            
            // Enviar notificação de cancelamento
            enviarNotificacaoAgendamentoCancelado(salvo, motivo);
            
            return Optional.of(salvo);
        });
    }
    
    /**
     * Envia notificação via WhatsApp quando agendamento é cancelado
     */
    private void enviarNotificacaoAgendamentoCancelado(Agendamento agendamento, String motivo) {
        try {
            Cliente cliente = agendamento.getUsuario();
            Servico servico = agendamento.getServico();
            
            if (cliente == null || cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
                log.debug("[WHATSAPP] Cliente sem telefone - cancelamento agendamento {}", agendamento.getId());
                return;
            }
            
            String dataFormatada = agendamento.getDataHora().format(DATE_FORMATTER);
            String mensagem = String.format(
                "❌ *Agendamento Cancelado*\n\n" +
                "Olá %s, infelizmente seu agendamento foi cancelado.\n\n" +
                "📋 *Serviço:* %s\n" +
                "📅 *Data:* %s\n" +
                "%s\n\n" +
                "Entre em contato conosco para reagendar.",
                cliente.getNome(),
                servico != null ? servico.getNome() : "Não especificado",
                dataFormatada,
                motivo != null && !motivo.isBlank() 
                    ? "📝 *Motivo:* " + motivo
                    : ""
            );
            
            evolutionApiService.sendTextMessage(
                agendamento.getTenantId(),
                cliente.getTelefone(),
                mensagem
            );
            
            log.info("[WHATSAPP] Notificação de cancelamento enviada para {}", cliente.getTelefone());
            
        } catch (Exception e) {
            log.error("[WHATSAPP] Erro ao enviar notificação de cancelamento: {}", e.getMessage());
        }
    }

    public Optional<Agendamento> atualizar(Long id, AgendamentoRequest req)
    {
        return agendamentoRepository.findById(id).flatMap(a -> {
            
            if (a.getStatus() == StatusAgendamentoEnum.FINALIZADO)
                return Optional.empty();

            if (req.getServicoId() != null)
            {
                Optional<Servico> servicoOpt = servicoRepository.findById(req.getServicoId());
                
                if (servicoOpt.isEmpty()) 
                    return Optional.empty();

                a.setServico(servicoOpt.get());
            }

            // Atualiza usuário se informado
            if (req.getUsuarioId() != null) 
            {
                Optional<Cliente> usuarioOpt = usuarioRepository.findById(req.getUsuarioId());
                if (usuarioOpt.isEmpty()) return Optional.empty();
                a.setUsuario(usuarioOpt.get());
            }

            // Atualiza data/hora se informada
            if (req.getDataHora() != null) 
            {
                try 
                {
                    LocalDateTime dataHora = LocalDateTime.parse(req.getDataHora());
                    a.setDataHora(dataHora);
                } catch (DateTimeParseException ex) {
                    return Optional.empty();
                }
            }

            if (req.getObs() != null)
                a.setObs(req.getObs());

            return Optional.of(agendamentoRepository.save(a));
        });
    }
}

package com.example.grazy_back.service;

import java.util.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

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

@Service
public class AgendamentoService 
{
    private final AgendamentoRepository agendamentoRepository;
    private final ServicoRepository servicoRepository;
    private final ClienteRepository usuarioRepository;
    private final TransacaoFinanceiraRepository transacaoRepository;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              ServicoRepository servicoRepository,
                              ClienteRepository usuarioRepository,
                              TransacaoFinanceiraRepository transacaoRepository)
    {
        this.agendamentoRepository = agendamentoRepository;
        this.servicoRepository = servicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.transacaoRepository = transacaoRepository;
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
        a.setServico(servicoOpt.get());
        a.setUsuario(usuarioOpt.get());
        a.setDataHora(dataHora);
        a.setObs(req.getObs());

        Agendamento salvo = agendamentoRepository.save(a);

        return Optional.of(salvo);
    }

    public List<Agendamento> listar() 
    {
        return agendamentoRepository.findAll();
    }

    public List<Agendamento> listarPorDia(LocalDateTime inicioDia)
    {
        LocalDateTime fimDia = inicioDia.plusDays(1);
        return agendamentoRepository.findByDataHoraBetweenOrderByDataHoraAsc(inicioDia, fimDia);
    }

    public long contarNoMes(int ano, int mes)
    {
        LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0, 0);
        LocalDateTime fim = inicio.plusMonths(1);
        return agendamentoRepository.countByDataHoraBetween(inicio, fim);
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
            return Optional.of(salvo);
        });
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

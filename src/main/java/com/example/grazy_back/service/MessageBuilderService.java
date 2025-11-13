package com.example.grazy_back.service;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.example.grazy_back.model.Agendamento;
import com.example.grazy_back.model.Cliente;

@Service
public class MessageBuilderService 
{
    private static final DateTimeFormatter DATA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public String assuntoLembreteAgendamento(Agendamento agendamento)
    {
        String servico = agendamento.getServico() != null ? agendamento.getServico().getNome() : "Serviço";
        String quando = agendamento.getDataHora() != null ? agendamento.getDataHora().format(DATA_HORA_FMT) : "";
        return "Lembrete de agendamento - " + servico + (quando.isBlank() ? "" : " (" + quando + ")");
    }

    public String corpoLembreteAgendamentoTexto(Cliente usuario, Agendamento agendamento)
    {
        String nome = usuario != null && usuario.getNome() != null ? usuario.getNome() : "Cliente";
        String servico = agendamento.getServico() != null ? agendamento.getServico().getNome() : "serviço";
        String quando = agendamento.getDataHora() != null ? agendamento.getDataHora().format(DATA_HORA_FMT) : "data/hora";
        return String.format("Olá %s, lembrete do seu agendamento de %s em %s.", nome, servico, quando);
    }

    public String assuntoResumoAgendamentos(java.time.LocalDate dia)
    {
        return "Resumo de agendamentos - " + dia.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String corpoResumoAgendamentos(java.util.List<Agendamento> lista, java.time.LocalDate dia)
    {
        if (lista == null || lista.isEmpty())
            return "Nenhum agendamento para o dia " + dia.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".";

        StringBuilder sb = new StringBuilder();
        sb.append("Resumo de agendamentos do dia ")
          .append(dia.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
          .append("\n\nTotal: ").append(lista.size()).append("\n\n");

        for (Agendamento a : lista)
        {
            String horario = a.getDataHora() != null ? a.getDataHora().format(DATA_HORA_FMT) : "--";
            String cliente = a.getUsuario() != null ? a.getUsuario().getNome() : "(sem cliente)";
            String servico = a.getServico() != null ? a.getServico().getNome() : "(sem serviço)";
            sb.append(horario).append(" | ")
              .append(cliente).append(" | ")
              .append(servico);
            if (a.getStatus() != null)
                sb.append(" | ").append(a.getStatus());
            sb.append('\n');
        }

        return sb.toString();
    }
}

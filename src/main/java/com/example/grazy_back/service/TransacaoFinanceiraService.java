package com.example.grazy_back.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.grazy_back.dto.TransacaoFinanceiraRequest;
import com.example.grazy_back.dto.ValoresDTO;
import com.example.grazy_back.enums.TipoTransacaoEnum;
import com.example.grazy_back.model.TransacaoFinanceira;
import com.example.grazy_back.repository.TransacaoFinanceiraRepository;

import jakarta.transaction.Transactional;

@Service
public class TransacaoFinanceiraService 
{
    @Autowired
    private TransacaoFinanceiraRepository transacaoRepository;

    @Transactional
    public ResponseEntity<?> criarTransacao(TransacaoFinanceiraRequest transacao)
    {
        if (transacao == null) 
            return ResponseEntity.badRequest().body("Transação inválida");

        TransacaoFinanceira novaTransacao = new TransacaoFinanceira();
        novaTransacao.setId(transacao.getId());
        novaTransacao.setDescricao(transacao.getDescricao());
        novaTransacao.setValor(transacao.getValor());
        novaTransacao.setTipo(transacao.getTipo());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate data = LocalDate.parse(transacao.getData(), formatter);
        novaTransacao.setData(Date.from(data.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        transacaoRepository.save(novaTransacao);
        return ResponseEntity.ok(novaTransacao);
    }

    public ResponseEntity<?> listarTransacoes() 
    {
        return ResponseEntity.ok(transacaoRepository.findAll());
    }

    public ResponseEntity<?> buscarValores()
    {
        double receita = transacaoRepository.findByTipo(TipoTransacaoEnum.RECEITA)
                .stream()
                .mapToDouble(TransacaoFinanceira::getValor)
                .sum();

        double despesa = transacaoRepository.findByTipo(TipoTransacaoEnum.DESPESA)
                .stream()
                .mapToDouble(TransacaoFinanceira::getValor)
                .sum();

        double lucroLiquido = receita - despesa;

        return ResponseEntity.ok(new ValoresDTO(receita, despesa, lucroLiquido));
    }
}

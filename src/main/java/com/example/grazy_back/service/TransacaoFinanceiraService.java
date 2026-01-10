package com.example.grazy_back.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.dto.TransacaoFinanceiraRequest;
import com.example.grazy_back.dto.ValoresDTO;
import com.example.grazy_back.enums.TipoTransacaoEnum;
import com.example.grazy_back.model.TransacaoFinanceira;
import com.example.grazy_back.repository.TransacaoFinanceiraRepository;
import com.example.grazy_back.security.TenantContext;

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
        novaTransacao.setTenantId(TenantContext.getCurrentTenantId());
        novaTransacao.setId(transacao.getId());
        novaTransacao.setDescricao(transacao.getDescricao());
        novaTransacao.setValor(transacao.getValor());
        novaTransacao.setTipo(transacao.getTipo());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate data = LocalDate.parse(transacao.getData(), formatter);
        novaTransacao.setData(Date.from(data.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        transacaoRepository.save(novaTransacao);
        return ResponseEntity.ok(ApiResposta.of(novaTransacao));
    }

    public ResponseEntity<?> listarTransacoes() 
    {
        Long tenantId = TenantContext.getCurrentTenantId();
        
        List<TransacaoFinanceira> transacoes;
        if (tenantId == null && TenantContext.isSuperAdmin()) 
        {
            transacoes = transacaoRepository.findAll();
        } 
        else 
        {
            transacoes = transacaoRepository.findByTenantId(tenantId);
        }
        
        return ResponseEntity.ok(ApiResposta.of(transacoes));
    }

    public ResponseEntity<?> buscarValores()
    {
        Long tenantId = TenantContext.getCurrentTenantId();
        
        List<TransacaoFinanceira> receitas;
        List<TransacaoFinanceira> despesas;
        
        if (tenantId == null && TenantContext.isSuperAdmin()) 
        {
            receitas = transacaoRepository.findByTipo(TipoTransacaoEnum.RECEITA);
            despesas = transacaoRepository.findByTipo(TipoTransacaoEnum.DESPESA);
        } 
        else 
        {
            receitas = transacaoRepository.findByTenantIdAndTipo(tenantId, TipoTransacaoEnum.RECEITA);
            despesas = transacaoRepository.findByTenantIdAndTipo(tenantId, TipoTransacaoEnum.DESPESA);
        }
        
        double receita = receitas.stream()
                .mapToDouble(TransacaoFinanceira::getValor)
                .sum();

        double despesa = despesas.stream()
                .mapToDouble(TransacaoFinanceira::getValor)
                .sum();

        double lucroLiquido = receita - despesa;

        return ResponseEntity.ok(ApiResposta.of(new ValoresDTO(receita, despesa, lucroLiquido)));
    }
}

package com.example.grazy_back.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.dto.TransacaoFinanceiraRequest;
import com.example.grazy_back.service.TransacaoFinanceiraService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *
 * @author Rubens
 */
@RestController
@RequestMapping("/api/transacoes")
@Tag(name = "Transações", description = "Controle de transações financeiras")
public class TransacaoFinanceiraController 
{
    @Autowired
    private TransacaoFinanceiraService transacaoService;

    @PostMapping
    @Operation(summary = "Cria uma transação")
    public ResponseEntity<?> criarTransacao(@RequestBody TransacaoFinanceiraRequest transacao) 
    {
        return transacaoService.criarTransacao(transacao);
    }

    @GetMapping
    @Operation(summary = "Lista transações")
    public ResponseEntity<?> listarTransacoes() 
    {
        return transacaoService.listarTransacoes();
    }

    @GetMapping("/valores")
    @Operation(summary = "Obtém valores de entrada/saída e saldo")
    public ResponseEntity<?> buscarValores()
    {
        return transacaoService.buscarValores();
    }
}

package com.example.grazy_back.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.dto.PagamentoRequest;
import com.example.grazy_back.dto.PagamentoResponse;
import com.example.grazy_back.enums.StatusPagamentoEnum;
import com.example.grazy_back.model.Pagamento;
import com.example.grazy_back.repository.PagamentoRepository;
import com.example.grazy_back.security.TenantContext;
import com.example.grazy_back.service.MercadoPagoService;

/**
 * Controller para gerenciamento de pagamentos
 */
@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {
    
    private final PagamentoRepository pagamentoRepository;
    private final MercadoPagoService mercadoPagoService;
    
    public PagamentoController(
        PagamentoRepository pagamentoRepository,
        MercadoPagoService mercadoPagoService
    ) {
        this.pagamentoRepository = pagamentoRepository;
        this.mercadoPagoService = mercadoPagoService;
    }
    
    /**
     * Listar todos os pagamentos do tenant logado
     */
    @GetMapping
    public ResponseEntity<ApiResposta<List<Pagamento>>> listar() {
        Long tenantId = TenantContext.getCurrentTenantId();
        List<Pagamento> pagamentos = pagamentoRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(ApiResposta.of(pagamentos));
    }
    
    /**
     * Buscar pagamento atual (do mês corrente)
     */
    @GetMapping("/atual")
    public ResponseEntity<ApiResposta<Pagamento>> buscarPagamentoAtual() {
        Long tenantId = TenantContext.getCurrentTenantId();
        LocalDate hoje = LocalDate.now();
        
        Pagamento pagamento = pagamentoRepository.findByTenantIdAndMesReferenciaAndAnoReferencia(
            tenantId, 
            hoje.getMonthValue(), 
            hoje.getYear()
        ).orElse(null);
        
        return ResponseEntity.ok(ApiResposta.of(pagamento));
    }
    
    /**
     * Criar um novo pagamento manualmente
     */
    @PostMapping
    public ResponseEntity<ApiResposta<PagamentoResponse>> criar(@RequestBody PagamentoRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        
        // Verifica se já existe pagamento para este mês/ano
        Pagamento existente = pagamentoRepository.findByTenantIdAndMesReferenciaAndAnoReferencia(
            tenantId, 
            request.getMesReferencia(), 
            request.getAnoReferencia()
        ).orElse(null);
        
        if (existente != null) {
            return ResponseEntity.ok(ApiResposta.error(
                "Já existe um pagamento para " + request.getMesReferencia() + "/" + request.getAnoReferencia()
            ));
        }
        
        // Cria o pagamento
        Pagamento pagamento = new Pagamento();
        pagamento.setTenantId(tenantId);
        pagamento.setValor(request.getValor());
        pagamento.setMesReferencia(request.getMesReferencia());
        pagamento.setAnoReferencia(request.getAnoReferencia());
        pagamento.setStatus(StatusPagamentoEnum.PENDENTE);
        
        // Define data de vencimento (hoje + 3 dias)
        pagamento.setDataVencimento(LocalDate.now().plusDays(3).atStartOfDay());
        
        if (request.getDescricao() != null) {
            pagamento.setObservacoes(request.getDescricao());
        }
        
        pagamento = pagamentoRepository.save(pagamento);
        
        // Cria preferência no Mercado Pago
        PagamentoResponse response = mercadoPagoService.criarPreferenciaPagamento(pagamento);
        
        return ResponseEntity.ok(ApiResposta.of(response));
    }
    
    /**
     * Webhook do Mercado Pago (rota pública)
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestParam(required = false) String type, 
                                       @RequestParam(required = false) String data_id,
                                       @RequestBody(required = false) Map<String, Object> payload) {
        
        // Mercado Pago pode enviar de duas formas: query params ou body
        String paymentType = type != null ? type : (String) payload.get("type");
        String paymentId = data_id != null ? data_id : 
            (payload.get("data") != null ? 
                ((Map<String, String>) payload.get("data")).get("id") : null);
        
        mercadoPagoService.processarWebhook(paymentType, paymentId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Buscar detalhes de um pagamento específico
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResposta<Pagamento>> buscar(@PathVariable Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        
        Pagamento pagamento = pagamentoRepository.findById(id).orElse(null);
        
        if (pagamento == null || !pagamento.getTenantId().equals(tenantId)) {
            return ResponseEntity.ok(ApiResposta.error("Pagamento não encontrado"));
        }
        
        return ResponseEntity.ok(ApiResposta.of(pagamento));
    }
}

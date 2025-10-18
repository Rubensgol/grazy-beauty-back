package com.example.grazy_back.controller;

import java.util.List;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.DTO.AgendamentoRequest;
import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.model.Agendamento;
import com.example.grazy_back.service.AgendamentoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/agendamentos")
@Tag(name = "Agendamentos", description = "Gerenciamento de agendamentos")
public class AgendamentoController 
{
    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service)
    {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria um agendamento")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agendamento criado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.example.grazy_back.model.Agendamento.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos ou IDs não encontrados", content = @Content)
    })
    public ResponseEntity<ApiResposta<Agendamento>> criar(@RequestBody AgendamentoRequest req) 
    {
        return service.criar(req)
                .map(a -> ResponseEntity.ok(ApiResposta.of(a)))
                .orElse(ResponseEntity.badRequest().body(ApiResposta.error("Dados inválidos ou IDs não encontrados")));
    }

    @GetMapping
    @Operation(summary = "Lista agendamentos", description = "Lista todos os agendamentos ou filtra por data (YYYY-MM-DD)")
    public ResponseEntity<ApiResposta<List<Agendamento>>> listar(@Parameter(description = "Data no formato YYYY-MM-DD") @RequestParam(required = false) String data) 
    {
        if (data == null || data.isBlank()) 
            return ResponseEntity.ok(ApiResposta.of(service.listar()));

        try 
        {
            LocalDate dia = LocalDate.parse(data); // formato ISO YYYY-MM-DD
            return ResponseEntity.ok(ApiResposta.of(service.listarPorDia(dia.atStartOfDay())));
        } 
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body(ApiResposta.error("Data inválida. Use formato YYYY-MM-DD"));
        }
    }

    @GetMapping("/estatistica/mes")
    @Operation(summary = "Contagem mensal de agendamentos")
    public ResponseEntity<ApiResposta<Long>> contarNoMes(@Parameter(description = "Ano, padrão = atual") @RequestParam(required = false) Integer ano,
                                                         @Parameter(description = "Mês (1-12), padrão = atual") @RequestParam(required = false) Integer mes) 
    {
            LocalDate now = LocalDate.now();
            int y = (ano == null) ? now.getYear() : ano;
            int m = (mes == null) ? now.getMonthValue() : mes;

            if (m < 1 || m > 12) 
                return ResponseEntity.badRequest().body(ApiResposta.error("Mes inválido (1-12)"));

            long total = service.contarNoMes(y, m);
        return ResponseEntity.ok(ApiResposta.of(total));
    }

    @PutMapping("/{id}/finalizar")
    @Operation(summary = "Finaliza um agendamento")
    public ResponseEntity<ApiResposta<Agendamento>> finalizar(@PathVariable Long id) 
    {
        return service.finalizar(id)
                .map(a -> ResponseEntity.ok(ApiResposta.of(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um agendamento")
    public ResponseEntity<ApiResposta<Agendamento>> atualizar(@PathVariable Long id, @RequestBody AgendamentoRequest req)
    {
        return service.atualizar(id, req)
                .map(a -> ResponseEntity.ok(ApiResposta.of(a)))
                .orElse(ResponseEntity.badRequest().body(ApiResposta.error("Não foi possível atualizar (ID inexistente, finalizado ou dados inválidos)")));
    }
}

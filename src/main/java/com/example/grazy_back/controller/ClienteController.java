package com.example.grazy_back.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grazy_back.DTO.ClienteRequestDTO;
import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.model.Cliente;
import com.example.grazy_back.service.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/cliente")
@Tag(name = "Clientes", description = "Cadastro e gestão de Clientes")
public class ClienteController 
{
    private final ClienteService service;

    public ClienteController(ClienteService service) 
    {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria cliente",
        requestBody = @RequestBody(required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ClienteRequestDTO.class))),
        responses = {
            @ApiResponse(responseCode = "200", description = "Cliente criado",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiResposta.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content)
        })
    public ResponseEntity<ApiResposta<Cliente>> criar(@org.springframework.web.bind.annotation.RequestBody ClienteRequestDTO req) 
    {
        Cliente u = service.criar(req);
        return ResponseEntity.ok(ApiResposta.of(u));
    }

    @GetMapping
    @Operation(summary = "Lista clientes",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lista retornada",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiResposta.class)))
        })
    public ResponseEntity<ApiResposta<List<Cliente>>> listar() 
    {
        return ResponseEntity.ok(ApiResposta.of(service.listarTodos()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza cliente",
        parameters = {
            @Parameter(name = "id", description = "ID do usuário", required = true)
        },
        requestBody = @RequestBody(required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ClienteRequestDTO.class))),
        responses = {
            @ApiResponse(responseCode = "200", description = "Cliente atualizado",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiResposta.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content)
        })
    public ResponseEntity<ApiResposta<Cliente>> atualizar(@PathVariable Long id, @org.springframework.web.bind.annotation.RequestBody ClienteRequestDTO req) 
    {
        return service.atualizar(id, req)
                .map(u -> ResponseEntity.ok(ApiResposta.of(u)))
                .orElse(ResponseEntity.notFound().build());
    }
}

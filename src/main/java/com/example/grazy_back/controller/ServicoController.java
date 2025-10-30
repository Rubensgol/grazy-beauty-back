package com.example.grazy_back.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.example.grazy_back.dto.ApiResposta;
import com.example.grazy_back.dto.OrdenacaoServicosRequest;
import com.example.grazy_back.dto.ServicoRequest;
import com.example.grazy_back.model.Servico;
import com.example.grazy_back.enums.ServicoDeleteResultado;
import com.example.grazy_back.service.ServicoService;

import org.springframework.beans.factory.annotation.Autowired;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/servicos")
@Tag(name = "Serviços", description = "CRUD de serviços oferecidos")
public class ServicoController 
{
    @Autowired
    private ServicoService servicoService;

    @GetMapping
    @Operation(summary = "Lista serviços")
    public ResponseEntity<ApiResposta<List<Servico>>> listarServicos() 
    {
        List<Servico> servicos = servicoService.listarServicos();
        return ResponseEntity.ok(ApiResposta.of(servicos));
    }

    @GetMapping("/todos")
    @Operation(summary = "Lista todos os serviços (ativos e inativos)")
    public ResponseEntity<ApiResposta<List<Servico>>> listarTodosServicos()
    {
        List<Servico> servicos = servicoService.listarTodosServicos();
        return ResponseEntity.ok(ApiResposta.of(servicos));
    }

    @PostMapping
    @Operation(summary = "Cria serviço")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Serviço criado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.example.grazy_back.dto.ApiResposta.class)))
    public ResponseEntity<ApiResposta<Servico>> criarServico(@RequestBody ServicoRequest servico) 
    {
        Servico novoServico = servicoService.salvarServico(servico);
        return ResponseEntity.ok(ApiResposta.of(novoServico));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza serviço")
    public ResponseEntity<ApiResposta<Servico>> atualizarServico(@PathVariable Long id, @RequestBody ServicoRequest servico)
    {
        Servico atualizado = servicoService.atualizarServico(id, servico);
        if (atualizado == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResposta.of(atualizado));
    }

    @PutMapping("/ordenacao")
    @Operation(summary = "Atualiza ordenação dos serviços")
    public ResponseEntity<ApiResposta<Void>> atualizarOrdenacao(@RequestBody OrdenacaoServicosRequest request)
    {
        servicoService.atualizarOrdenacao(request.getIds());
        return ResponseEntity.ok(ApiResposta.of(null));
    }

    @PutMapping("/{id}/ativar")
    @Operation(summary = "Reativa serviço inativo")
    public ResponseEntity<ApiResposta<Servico>> ativarServico(@PathVariable Long id)
    {
        Servico ativado = servicoService.ativarServico(id);
        if (ativado == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResposta.of(ativado));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui serviço")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Serviço excluído"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<ApiResposta<Void>> deletarServico(@PathVariable Long id) 
    {
        ServicoDeleteResultado resultado = servicoService.deletarServicoComResultado(id);

        switch (resultado)
        {
            case NAO_ENCONTRADO:
                return ResponseEntity.notFound().build();
            case DESATIVADO:
                return ResponseEntity.ok(new ApiResposta<Void>(true, null, "Serviço desativado", java.time.Instant.now()));
            case JA_INATIVO:
                return ResponseEntity.ok(new ApiResposta<Void>(true, null, "Serviço já estava desativado", java.time.Instant.now()));
            case EXCLUIDO:
            default:
                return ResponseEntity.ok(new ApiResposta<Void>(true, null, "Serviço excluído", java.time.Instant.now()));
        }
    }
}

package com.example.grazy_back.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import org.springframework.data.domain.Sort;

import com.example.grazy_back.dto.ServicoRequest;
import com.example.grazy_back.enums.ServicoDeleteResultado;
import com.example.grazy_back.model.Servico;
import com.example.grazy_back.repository.ServicoRepository;
import com.example.grazy_back.repository.AgendamentoRepository;

@Service
public class ServicoService 
{
    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    public List<Servico> listarServicos() 
    {
        return servicoRepository.findByAtivoTrue(Sort.by(Sort.Direction.ASC, "ordem", "nome"));
    }

    public Servico buscarServicoPorId(Long id) 
    {
        return servicoRepository.findById(id).orElse(null);
    }

    public Servico salvarServico(ServicoRequest servico) 
    {
        Servico novoServico = new Servico();
        novoServico.setNome(servico.getNome());
        novoServico.setDescricao(servico.getDescricao());
        novoServico.setPreco(servico.getPreco());
        novoServico.setDuracaoMinutos(servico.getDuracaoMinutos());
        novoServico.setAtivo(true);
        // se não vier ordem, coloca no final
        if (servico.getOrdem() != null)
            novoServico.setOrdem(servico.getOrdem());
        else
            novoServico.setOrdem(proximaOrdem());
        
        if (servico.getStoredFilename() != null) 
            novoServico.setImageStoredFilename(servico.getStoredFilename());

        return servicoRepository.save(novoServico);
    }

    public Servico atualizarServico(Long id, ServicoRequest servico) 
    {
        return servicoRepository.findById(id).map(existing -> {
            if (servico.getNome() != null) existing.setNome(servico.getNome());
            if (servico.getDescricao() != null) existing.setDescricao(servico.getDescricao());
            if (servico.getPreco() != null) existing.setPreco(servico.getPreco());
            if (servico.getStoredFilename() != null) existing.setImageStoredFilename(servico.getStoredFilename());
            if (servico.getDuracaoMinutos() != null) existing.setDuracaoMinutos(servico.getDuracaoMinutos());
            if (servico.getOrdem() != null) existing.setOrdem(servico.getOrdem());
            return servicoRepository.save(existing);
        }).orElse(null);
    }

    public boolean deletarServico(Long id)
    {
        ServicoDeleteResultado r = deletarServicoComResultado(id);
        return r != ServicoDeleteResultado.NAO_ENCONTRADO;
    }

    public ServicoDeleteResultado deletarServicoComResultado(Long id)
    {
        if (!servicoRepository.existsById(id))
            return ServicoDeleteResultado.NAO_ENCONTRADO;

        Servico servico = servicoRepository.findById(id).orElse(null);

        if (servico == null)
            return ServicoDeleteResultado.NAO_ENCONTRADO;

        boolean temAgendamentos = agendamentoRepository.existsByServicoId(id);

        if (temAgendamentos) 
        {
            if (Boolean.FALSE.equals(servico.getAtivo()))
                return ServicoDeleteResultado.JA_INATIVO;

            servico.setAtivo(false);
            servicoRepository.save(servico);
            return ServicoDeleteResultado.DESATIVADO;
        }

        String stored = servico.getImageStoredFilename();
        servicoRepository.deleteById(id);

        if (stored != null && !stored.isBlank()) 
        {
            try 
            {
                imageStorageService.deleteByStoredFilename(stored);
            } 
            catch (IOException e) 
            {
                System.err.println("Failed to delete stored image '" + stored + "': " + e.getMessage());
            }
        }

        return ServicoDeleteResultado.EXCLUIDO;
    }

    private int proximaOrdem()
    {
        return servicoRepository.findAll().stream()
                .map(Servico::getOrdem)
                .filter(o -> o != null)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(0);
    }

    public void atualizarOrdenacao(List<Long> idsOrdenados)
    {
        List<Servico> servicos = servicoRepository.findAllById(idsOrdenados);
        Map<Long, Servico> porId = servicos.stream().collect(Collectors.toMap(Servico::getId, s -> s));

        for (int i = 0; i < idsOrdenados.size(); i++)
        {
            Long id = idsOrdenados.get(i);
            Servico s = porId.get(id);
            if (s != null) s.setOrdem(i);
        }

        servicoRepository.saveAll(porId.values());
    }

    public Servico ativarServico(Long id)
    {
        return servicoRepository.findById(id)
            .map(s -> {
                if (Boolean.TRUE.equals(s.getAtivo())) return s; // idempotente
                s.setAtivo(true);
                return servicoRepository.save(s);
            })
            .orElse(null);
    }

    public List<Servico> listarTodosServicos()
    {
        return servicoRepository.findAll(Sort.by(Sort.Direction.ASC, "ordem", "nome"));
    }

}

package com.example.grazy_back.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;

import com.example.grazy_back.dto.ServicoRequest;
import com.example.grazy_back.model.Servico;
import com.example.grazy_back.repository.ServicoRepository;

@Service
public class ServicoService 
{
    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    public List<Servico> listarServicos() 
    {
        return servicoRepository.findAll();
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
            return servicoRepository.save(existing);
        }).orElse(null);
    }

    public boolean deletarServico(Long id)
    {
        if (!servicoRepository.existsById(id))
            return false;

        Servico servico = servicoRepository.findById(id).orElse(null);

        if (servico == null)
            return false;

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

        return true;
    }

}

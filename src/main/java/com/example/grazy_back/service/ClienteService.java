package com.example.grazy_back.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.grazy_back.DTO.ClienteRequestDTO;
import com.example.grazy_back.model.Cliente;
import com.example.grazy_back.repository.ClienteRepository;

@Service
public class ClienteService 
{
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) 
    {
        this.repository = repository;
    }

    public Cliente criar(ClienteRequestDTO req) 
    {
        Cliente u = new Cliente();
        u.setNome(req.getNome());
        u.setTelefone(req.getTelefone());
        u.setEmail(req.getEmail());
        u.setObs(req.getObs());
        return repository.save(u);
    }

    public List<Cliente> listarTodos()
    {
        return repository.findAll();
    }

    public Optional<Cliente> atualizar(Long id, ClienteRequestDTO req) 
    {
        return repository.findById(id).map(u -> {
            if (req.getNome() != null) u.setNome(req.getNome());
            if (req.getTelefone() != null) u.setTelefone(req.getTelefone());
            if (req.getEmail() != null) u.setEmail(req.getEmail());
            if (req.getObs() != null) u.setObs(req.getObs());
            return repository.save(u);
        });
    }
}

package com.example.grazy_back.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.grazy_back.DTO.ClienteRequestDTO;
import com.example.grazy_back.model.Cliente;
import com.example.grazy_back.repository.ClienteRepository;
import com.example.grazy_back.security.TenantContext;

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
        u.setTenantId(TenantContext.getCurrentTenantId());
        u.setNome(req.getNome());
        u.setTelefone(req.getTelefone());
        u.setEmail(req.getEmail());
        u.setObs(req.getObs());
        return repository.save(u);
    }

    public List<Cliente> listarTodos()
    {
        Long tenantId = TenantContext.getCurrentTenantId();
        
        // SUPER_ADMIN vê todos, tenant vê apenas os seus
        if (tenantId == null && TenantContext.isSuperAdmin()) 
        {
            return repository.findAll();
        }
        
        return repository.findByTenantId(tenantId);
    }

    public Optional<Cliente> atualizar(Long id, ClienteRequestDTO req) 
    {
        return repository.findById(id).map(u -> {
            // Verificar se cliente pertence ao tenant
            Long tenantId = TenantContext.getCurrentTenantId();
            if (tenantId != null && !tenantId.equals(u.getTenantId())) 
            {
                throw new IllegalStateException("Cliente não pertence ao seu tenant");
            }
            
            if (req.getNome() != null) u.setNome(req.getNome());
            if (req.getTelefone() != null) u.setTelefone(req.getTelefone());
            if (req.getEmail() != null) u.setEmail(req.getEmail());
            if (req.getObs() != null) u.setObs(req.getObs());
            return repository.save(u);
        });
    }
}

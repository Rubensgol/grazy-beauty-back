package com.example.grazy_back.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.grazy_back.model.Usuario;
import com.example.grazy_back.model.Tenant;
import com.example.grazy_back.enums.RoleEnum;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> 
{
    Optional<Usuario> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<Usuario> findByTenant(Tenant tenant);
    
    List<Usuario> findByTenantId(Long tenantId);
    
    List<Usuario> findByRole(RoleEnum role);
    
    List<Usuario> findByTenantAndAtivoTrue(Tenant tenant);
    
    Optional<Usuario> findByTokenRecuperacaoSenha(String token);
}

package com.example.grazy_back.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.grazy_back.model.Tenant;
import com.example.grazy_back.enums.StatusTenantEnum;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> 
{
    Optional<Tenant> findBySubdominio(String subdominio);
    
    Optional<Tenant> findByDominioCustomizado(String dominioCustomizado);
    
    Optional<Tenant> findByEmailAdmin(String emailAdmin);
    
    boolean existsBySubdominio(String subdominio);
    
    boolean existsByEmailAdmin(String emailAdmin);
    
    List<Tenant> findByAtivoTrue();
    
    List<Tenant> findByStatus(StatusTenantEnum status);
    
    List<Tenant> findByAtivoTrueOrderByCriadoEmDesc();
}

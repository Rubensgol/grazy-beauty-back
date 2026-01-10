package com.example.grazy_back.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.grazy_back.model.ConfiguracaoTenant;
import com.example.grazy_back.model.Tenant;

@Repository
public interface ConfiguracaoTenantRepository extends JpaRepository<ConfiguracaoTenant, Long> 
{
    Optional<ConfiguracaoTenant> findByTenant(Tenant tenant);
    
    Optional<ConfiguracaoTenant> findByTenantId(Long tenantId);
}

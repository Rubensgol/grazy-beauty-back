package com.example.grazy_back.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.grazy_back.model.ConteudoSite;

/**
 * Repository para ConteudoSite.
 * Gerencia o conteúdo do site (landing page) de cada tenant.
 */
@Repository
public interface ConteudoSiteRepository extends JpaRepository<ConteudoSite, Long> 
{
    /**
     * Busca conteúdo do site pelo tenant ID.
     */
    Optional<ConteudoSite> findByTenantId(Long tenantId);

    /**
     * Verifica se existe conteúdo para o tenant.
     */
    boolean existsByTenantId(Long tenantId);

    /**
     * Remove conteúdo do tenant.
     */
    void deleteByTenantId(Long tenantId);
}

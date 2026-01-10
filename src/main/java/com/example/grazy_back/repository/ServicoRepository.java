package com.example.grazy_back.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.example.grazy_back.model.Servico;
import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> 
{
	List<Servico> findByAtivoTrue(Sort sort);
	List<Servico> findByTenantId(Long tenantId);
	List<Servico> findByTenantIdAndAtivoTrue(Long tenantId, Sort sort);
}
package com.example.grazy_back.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.grazy_back.enums.TipoTransacaoEnum;
import com.example.grazy_back.model.TransacaoFinanceira;

/**
 *
 * @author Rubens
 */
@Repository
public interface TransacaoFinanceiraRepository extends JpaRepository<TransacaoFinanceira, Long>
{
    List<TransacaoFinanceira> findByTipo(TipoTransacaoEnum tipo);
    List<TransacaoFinanceira> findByTenantId(Long tenantId);
    List<TransacaoFinanceira> findByTenantIdAndTipo(Long tenantId, TipoTransacaoEnum tipo);
}

package com.example.grazy_back.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.grazy_back.enums.StatusPagamentoEnum;
import com.example.grazy_back.model.Pagamento;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    
    List<Pagamento> findByTenantId(Long tenantId);
    
    List<Pagamento> findByTenantIdAndStatus(Long tenantId, StatusPagamentoEnum status);
    
    Optional<Pagamento> findByTenantIdAndMesReferenciaAndAnoReferencia(
        Long tenantId, Integer mesReferencia, Integer anoReferencia
    );
    
    List<Pagamento> findByStatusAndDataVencimentoBefore(
        StatusPagamentoEnum status, LocalDateTime dataVencimento
    );
    
    List<Pagamento> findByStatusAndEnviadoWhatsappFalseAndEnviadoEmailFalse(
        StatusPagamentoEnum status
    );
    
    Optional<Pagamento> findByMercadoPagoId(String mercadoPagoId);
}

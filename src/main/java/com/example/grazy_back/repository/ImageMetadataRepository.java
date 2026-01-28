package com.example.grazy_back.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.grazy_back.model.ImageMetadata;

public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, Long> 
{
	Optional<ImageMetadata> findByStoredFilename(String storedFilename);
	List<ImageMetadata> findAllByForServicoFalse();
	List<ImageMetadata> findByTenantId(Long tenantId);
	List<ImageMetadata> findByTenantIdAndForServicoFalse(Long tenantId);
	
	// Para landing page
	List<ImageMetadata> findAllByForServicoFalseAndExibirLandingTrueOrderByOrdemLandingAsc();
	List<ImageMetadata> findByTenantIdAndForServicoFalseAndExibirLandingTrueOrderByOrdemLandingAsc(Long tenantId);
}

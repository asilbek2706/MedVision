package com.medvision.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

    Page<Analysis> findByImage_Id(UUID imageId, Pageable pageable);

    Page<Analysis> findByImage_User_Id(UUID userId, Pageable pageable);

    Optional<Analysis> findByIdAndImage_User_Id(UUID id, UUID userId);

    long countByImage_User_IdAndStatus(UUID userId, AnalysisStatus status);
}
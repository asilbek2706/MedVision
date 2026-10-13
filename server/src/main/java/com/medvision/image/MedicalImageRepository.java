package com.medvision.image;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedicalImageRepository
        extends JpaRepository<MedicalImage, UUID> {

    Page<MedicalImage> findByUser_Id(UUID userId, Pageable pageable);

    Optional<MedicalImage> findByIdAndUser_Id(UUID id, UUID userId);

    long countByUser_Id(UUID userId);
}
package com.medvision.analysis;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

    Page<Analysis> findByImage_Id(UUID imageId, Pageable pageable);

    Page<Analysis> findByImage_User_Id(UUID userId, Pageable pageable);

    Optional<Analysis> findByIdAndImage_User_Id(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM Analysis a
            WHERE a.id = :id
              AND a.image.user.id = :ownerId
            """)
    Optional<Analysis> findOwnedForEvaluationUpdate(
            @Param("id") UUID id,
            @Param("ownerId") UUID ownerId
    );

    long countByImage_User_IdAndStatus(UUID userId, AnalysisStatus status);
    @Query("""
            SELECT a FROM Analysis a
            WHERE a.image.user.id = :ownerId
              AND (:status IS NULL OR a.status = :status)
              AND (:algorithm IS NULL OR a.algorithm = :algorithm)
              AND (a.createdAt >= COALESCE(:fromDate, a.createdAt))
              AND (a.createdAt <= COALESCE(:toDate, a.createdAt))
            """)
    Page<Analysis> findFilteredByOwner(
            @Param("ownerId") UUID ownerId,
            @Param("status") AnalysisStatus status,
            @Param("algorithm") String algorithm,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );
}
package com.medvision.security;

import com.medvision.analysis.*;
import com.medvision.image.*;
import com.medvision.common.error.ApiException;
import com.medvision.common.pagination.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OwnedResourceService {

    private final MedicalImageRepository images;
    private final AnalysisRepository analyses;

    public OwnedResourceService(
            MedicalImageRepository images,
            AnalysisRepository analyses
    ) {
        this.images = images;
        this.analyses = analyses;
    }

    public PageResponse<MedicalImageResponse> images(
            UUID ownerId, int page, int size
    ) {
        return PageResponse.from(
                images.findByUser_Id(ownerId, pageRequest(page, size))
                        .map(MedicalImageResponse::from)
        );
    }

    public MedicalImageResponse image(UUID ownerId, UUID id) {
        return MedicalImageResponse.from(
                images.findByIdAndUser_Id(id, ownerId)
                        .orElseThrow(OwnedResourceService::notFound)
        );
    }

    public PageResponse<AnalysisResponse> analyses(
            UUID ownerId, int page, int size,
            AnalysisStatus status, String algorithm,
            Instant from, Instant to
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE",
                    "Boshlanish sanasi tugash sanasidan keyin bo'lmasligi kerak."
            );
        }

        if (algorithm != null) {
            algorithm = algorithm.trim().toUpperCase(java.util.Locale.ROOT);

            if (!algorithm.equals("OTSU")) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ALGORITHM",
                        "Hozircha faqat OTSU algoritmi qo'llab-quvvatlanadi."
                );
            }
        }

        return PageResponse.from(
                analyses.findFilteredByOwner(
                        ownerId, status, algorithm, from, to,
                        pageRequest(page, size)
                ).map(AnalysisResponse::from)
        );
    }

    public AnalysisResponse analysis(UUID ownerId, UUID id) {
        return AnalysisResponse.from(
                analyses.findByIdAndImage_User_Id(id, ownerId)
                        .orElseThrow(OwnedResourceService::notFound)
        );
    }

    public PageResponse<AnalysisResponse> imageAnalyses(
            UUID ownerId, UUID imageId, int page, int size
    ) {
        var pageable = pageRequest(page, size);

        images.findByIdAndUser_Id(imageId, ownerId)
                .orElseThrow(OwnedResourceService::notFound);

        return PageResponse.from(
                analyses.findByImage_Id(imageId, pageable)
                        .map(AnalysisResponse::from)
        );
    }

    public DashboardStatsResponse dashboardStats(UUID ownerId) {
        return new DashboardStatsResponse(
                images.countByUser_Id(ownerId),
                analyses.countByImage_User_IdAndStatus(
                        ownerId, AnalysisStatus.COMPLETED),
                analyses.countByImage_User_IdAndStatus(
                        ownerId, AnalysisStatus.FAILED)
        );
    }

    private static PageRequest pageRequest(int page, int size) {
        if (page < 0 || page > 10_000 || size < 1 || size > 100) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PAGINATION",
                    "Page 0-10000, size 1-100 oralig'ida bo'lishi kerak."
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
    }

    private static ApiException notFound() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                "So'ralgan resurs topilmadi."
        );
    }
}

package com.medvision.analysis;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AnalysisResponse(
        UUID id,
        UUID imageId,
        String name,
        String algorithm,
        Map<String, Object> parameters,
        AnalysisStatus status,
        Long foregroundPixels,
        Double foregroundRatio,
        Long processingTimeMs,
        Double diceScore,
        Double iouScore,
        Boolean hasGroundTruth,
        String errorCode,
        Instant createdAt,
        Instant completedAt
) {
    public static AnalysisResponse from(Analysis analysis) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getImage().getId(),
                analysis.getName(),
                analysis.getAlgorithm(),
                analysis.getParameters(),
                analysis.getStatus(),
                analysis.getForegroundPixels(),
                analysis.getForegroundRatio(),
                analysis.getProcessingTimeMs(),
                analysis.getDiceScore(),
                analysis.getIouScore(),
                analysis.getGroundTruthKey() != null,
                analysis.getErrorCode(),
                analysis.getCreatedAt(),
                analysis.getCompletedAt()
        );
    }
}
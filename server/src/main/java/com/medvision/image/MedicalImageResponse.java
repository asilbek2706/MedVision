package com.medvision.image;

import java.time.Instant;
import java.util.UUID;

public record MedicalImageResponse(
        UUID id,
        String originalName,
        String mimeType,
        long sizeBytes,
        int width,
        int height,
        Instant createdAt
) {
    public static MedicalImageResponse from(MedicalImage image) {
        return new MedicalImageResponse(
                image.getId(),
                image.getOriginalName(),
                image.getMimeType(),
                image.getSizeBytes(),
                image.getWidth(),
                image.getHeight(),
                image.getCreatedAt()
        );
    }
}
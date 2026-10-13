package com.medvision.analysis;

import com.medvision.image.MedicalImage;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private MedicalImage image;

    @Column(nullable = false, length = 50)
    private String algorithm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "mask_key", length = 500)
    private String maskKey;

    @Column(name = "overlay_key", length = 500)
    private String overlayKey;

    @Column(name = "foreground_pixels")
    private Long foregroundPixels;

    @Column(name = "foreground_ratio")
    private Double foregroundRatio;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Analysis() {
    }

    public Analysis(
            MedicalImage image,
            String algorithm,
            Map<String, Object> parameters
    ) {
        this.image = Objects.requireNonNull(image);
        this.algorithm = Objects.requireNonNull(algorithm).trim();

        if (this.algorithm.isEmpty() || this.algorithm.length() > 50) {
            throw new IllegalArgumentException("Invalid algorithm.");
        }

        this.parameters = Map.copyOf(Objects.requireNonNull(parameters));
        this.status = AnalysisStatus.PROCESSING;
        this.createdAt = Instant.now();
    }

    public void complete(
            String maskKey,
            String overlayKey,
            long foregroundPixels,
            double foregroundRatio,
            long processingTimeMs
    ) {
        requireProcessing();

        String validatedMask = requireKey(maskKey);
        String validatedOverlay = requireKey(overlayKey);

        long totalPixels = (long) image.getWidth() * image.getHeight();

        if (foregroundPixels < 0 || foregroundPixels > totalPixels
                || !Double.isFinite(foregroundRatio)
                || foregroundRatio < 0 || foregroundRatio > 1
                || processingTimeMs < 0) {
            throw new IllegalArgumentException("Invalid analysis statistics.");
        }

        this.maskKey = validatedMask;
        this.overlayKey = validatedOverlay;
        this.foregroundPixels = foregroundPixels;
        this.foregroundRatio = foregroundRatio;
        this.processingTimeMs = processingTimeMs;
        this.completedAt = Instant.now();
        this.status = AnalysisStatus.COMPLETED;
    }

    public void fail(String errorCode) {
        requireProcessing();

        if (errorCode == null || errorCode.isBlank()
                || errorCode.length() > 100) {
            throw new IllegalArgumentException("Invalid error code.");
        }

        this.errorCode = errorCode;
        this.completedAt = Instant.now();
        this.status = AnalysisStatus.FAILED;
    }

    private void requireProcessing() {
        if (status != AnalysisStatus.PROCESSING) {
            throw new IllegalStateException("Analysis is already finalized.");
        }
    }

    private String requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 500) {
            throw new IllegalArgumentException("Invalid result storage key.");
        }
        return key;
    }

    public UUID getId() { return id; }
    public MedicalImage getImage() { return image; }
    public String getAlgorithm() { return algorithm; }
    public Map<String, Object> getParameters() {
        return Map.copyOf(parameters);
    }
    public AnalysisStatus getStatus() { return status; }
    public String getMaskKey() { return maskKey; }
    public String getOverlayKey() { return overlayKey; }
    public Long getForegroundPixels() { return foregroundPixels; }
    public Double getForegroundRatio() { return foregroundRatio; }
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public String getErrorCode() { return errorCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
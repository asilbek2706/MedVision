package com.medvision.analysis;

import com.medvision.common.error.ApiException;
import com.medvision.image.MedicalImageRepository;
import com.medvision.segmentation.SegmentationService;
import com.medvision.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class AnalysisExecutionService {

    private final MedicalImageRepository images;
    private final AnalysisRepository analyses;
    private final StorageService storage;
    private final SegmentationService segmentation;
    private final TransactionTemplate transactions;

    public AnalysisExecutionService(
            MedicalImageRepository images,
            AnalysisRepository analyses,
            StorageService storage,
            SegmentationService segmentation,
            PlatformTransactionManager transactionManager
    ) {
        this.images = images;
        this.analyses = analyses;
        this.storage = storage;
        this.segmentation = segmentation;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public AnalysisResponse execute(
            UUID ownerId,
            UUID imageId,
            AnalysisParametersRequest request
    ) {
        if (request == null
                || !"OTSU".equals(request.algorithm())
                || request.blurKernelSize() == null
                || request.invert() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PARAMETERS",
                    "Invalid analysis parameters."
            );
        }

        int blur = request.blurKernelSize();

        if (blur < 1 || blur > 15 || blur % 2 == 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_BLUR_KERNEL",
                    "Blur kernel must be odd and between 1 and 15."
            );
        }

        var options = new SegmentationService.Options(
                request.invert(), blur, 3, 50
        );

        var source = transactions.execute(status ->
                images.findByIdAndUser_Id(imageId, ownerId)
                        .map(image -> new Source(
                                image.getStorageKey(),
                                image.getSizeBytes()
                        ))
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "NOT_FOUND",
                                "Image not found."
                        ))
        );

        if (source == null) {
            throw new IllegalStateException("Source metadata missing.");
        }

        byte[] input = storage.read(source.storageKey());

        if (input.length != source.sizeBytes()) {
            throw new IllegalStateException("Source size mismatch.");
        }

        var result = segmentation.segment(input, options);

        String maskKey = null;
        String overlayKey = null;

        try {
            maskKey = storage.store(result.maskPng(), "png");
            overlayKey = storage.store(result.overlayPng(), "png");

            final String finalMaskKey = maskKey;
            final String finalOverlayKey = overlayKey;

            var response = transactions.execute(status -> {
                var image = images.findByIdAndUser_Id(imageId, ownerId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "NOT_FOUND",
                                "Image not found."
                        ));

                var analysis = new Analysis(
                        image,
                        "OTSU",
                        Map.of(
                                "invert", request.invert(),
                                "blurKernelSize", blur,
                                "morphologySize", 3,
                                "minArea", 50
                        )
                );

                analysis.complete(
                        finalMaskKey,
                        finalOverlayKey,
                        result.foregroundPixels(),
                        result.foregroundPercent() / 100.0,
                        result.processingMillis()
                );

                return AnalysisResponse.from(
                        analyses.saveAndFlush(analysis)
                );
            });

            if (response == null) {
                throw new IllegalStateException(
                        "Analysis transaction returned no result."
                );
            }

            return response;
        } catch (RuntimeException ex) {
            cleanup(overlayKey, ex);
            cleanup(maskKey, ex);
            throw ex;
        }
    }

    private void cleanup(String key, RuntimeException original) {
        if (key == null) {
            return;
        }

        try {
            storage.delete(key);
        } catch (RuntimeException cleanupFailure) {
            original.addSuppressed(cleanupFailure);
        }
    }

    private record Source(String storageKey, long sizeBytes) {}
}
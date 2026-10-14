package com.medvision.analysis;

import com.medvision.common.error.ApiException;
import com.medvision.image.ImageUploadValidator;
import com.medvision.segmentation.evaluation.SegmentationEvaluationService;
import com.medvision.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class AnalysisEvaluationExecutionService {

    private final AnalysisRepository analyses;
    private final ImageUploadValidator validator;
    private final SegmentationEvaluationService evaluation;
    private final StorageService storage;
    private final TransactionTemplate transactions;

    public AnalysisEvaluationExecutionService(
            AnalysisRepository analyses,
            ImageUploadValidator validator,
            SegmentationEvaluationService evaluation,
            StorageService storage,
            PlatformTransactionManager transactionManager
    ) {
        this.analyses = analyses;
        this.validator = validator;
        this.evaluation = evaluation;
        this.storage = storage;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public AnalysisResponse evaluate(
            UUID ownerId,
            UUID analysisId,
            MultipartFile file
    ) {
        var source = transactions.execute(status -> {
            Analysis analysis = analyses
                    .findByIdAndImage_User_Id(analysisId, ownerId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "NOT_FOUND",
                            "Analysis not found."
                    ));

            if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "ANALYSIS_NOT_COMPLETED",
                        "Analysis must be completed before evaluation."
                );
            }

            if (analysis.getGroundTruthKey() != null) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "ALREADY_EVALUATED",
                        "Analysis has already been evaluated."
                );
            }

            return new Source(analysis.getMaskKey());
        });

        if (source == null) {
            throw new IllegalStateException(
                    "Analysis source metadata missing."
            );
        }

        var groundTruth = validator.validate(file);

        if (!"image/png".equals(groundTruth.mimeType())) {
            throw new ApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "GROUND_TRUTH_MUST_BE_PNG",
                    "Ground-truth mask must be a PNG image."
            );
        }

        byte[] predictedMask = storage.read(source.maskKey());

        var metrics = evaluateMasks(
                predictedMask,
                groundTruth.bytes()
        );

        String key = storage.store(groundTruth.bytes(), "png");

        try {
            AnalysisResponse response = transactions.execute(status -> {
                Analysis analysis = analyses
                        .findOwnedForEvaluationUpdate(analysisId, ownerId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "NOT_FOUND",
                                "Analysis not found."
                        ));

                if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "ANALYSIS_NOT_COMPLETED",
                            "Analysis must be completed before evaluation."
                    );
                }

                if (analysis.getGroundTruthKey() != null) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "ALREADY_EVALUATED",
                            "Analysis has already been evaluated."
                    );
                }

                analysis.completeEvaluation(
                        key,
                        metrics.dice(),
                        metrics.iou()
                );

                return AnalysisResponse.from(
                        analyses.saveAndFlush(analysis)
                );
            });

            if (response == null) {
                throw new IllegalStateException(
                        "Evaluation transaction returned no result."
                );
            }

            return response;
        } catch (RuntimeException ex) {
            try {
                storage.delete(key);
            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }

            throw ex;
        }
    }

    private com.medvision.segmentation.evaluation.SegmentationMetrics.Result
    evaluateMasks(byte[] predictedMask, byte[] groundTruthMask) {
        try {
            return evaluation.evaluate(
                    predictedMask,
                    groundTruthMask
            );
        } catch (IllegalArgumentException ex) {
            if ("Prediction and ground-truth dimensions must match."
                    .equals(ex.getMessage())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "GROUND_TRUTH_DIMENSIONS_MISMATCH",
                        "Ground-truth mask dimensions must match the predicted mask."
                );
            }

            throw ex;
        }
    }
    private record Source(String maskKey) {
    }
}
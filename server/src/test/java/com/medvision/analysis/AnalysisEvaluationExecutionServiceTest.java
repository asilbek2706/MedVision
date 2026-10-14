package com.medvision.analysis;

import com.medvision.common.error.ApiException;
import com.medvision.image.ImageUploadValidator;
import com.medvision.segmentation.evaluation.SegmentationEvaluationService;
import com.medvision.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnalysisEvaluationExecutionServiceTest {

    private final UUID ownerId = UUID.randomUUID();
    private final UUID analysisId = UUID.randomUUID();

    private final AnalysisRepository analyses =
            mock(AnalysisRepository.class);

    private final ImageUploadValidator validator =
            mock(ImageUploadValidator.class);

    private final SegmentationEvaluationService evaluation =
            mock(SegmentationEvaluationService.class);

    private final StorageService storage =
            mock(StorageService.class);

    private final PlatformTransactionManager manager =
            mock(PlatformTransactionManager.class);

    private AnalysisEvaluationExecutionService service() {
        when(manager.getTransaction(any()))
                .thenReturn(mock(TransactionStatus.class));

        return new AnalysisEvaluationExecutionService(
                analyses,
                validator,
                evaluation,
                storage,
                manager
        );
    }

    private MockMultipartFile file() {
        return new MockMultipartFile(
                "file",
                "ground-truth.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
    }

    private void validUpload(MockMultipartFile file) throws java.io.IOException {
        when(validator.validate(file))
                .thenReturn(new ImageUploadValidator.ValidatedImage(
                        file.getBytes(),
                        "ground-truth.png",
                        "image/png",
                        "png",
                        2,
                        2
                ));
    }

    @Test
    void rejectsAnalysisNotOwnedByUser() throws java.io.IOException {
        var upload = file();
        validUpload(upload);

        when(analyses.findByIdAndImage_User_Id(
                analysisId, ownerId
        )).thenReturn(Optional.empty());

        var ex = assertThrows(
                ApiException.class,
                () -> service().evaluate(ownerId, analysisId, upload)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("NOT_FOUND", ex.getCode());

        verify(storage, never()).store(any(), any());
    }

    @Test
    void rejectsAlreadyEvaluatedAnalysis() throws java.io.IOException {
        var upload = file();
        validUpload(upload);

        var analysis = mock(Analysis.class);

        when(analysis.getStatus())
                .thenReturn(AnalysisStatus.COMPLETED);

        when(analysis.getGroundTruthKey())
                .thenReturn("existing.png");

        when(analyses.findByIdAndImage_User_Id(
                analysisId, ownerId
        )).thenReturn(Optional.of(analysis));

        var ex = assertThrows(
                ApiException.class,
                () -> service().evaluate(ownerId, analysisId, upload)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("ALREADY_EVALUATED", ex.getCode());

        verify(storage, never()).store(any(), any());
    }

    @Test
    void cleansUpStoredGroundTruthWhenDatabaseFails() throws java.io.IOException {
        var upload = file();
        validUpload(upload);

        var analysis = mock(Analysis.class);

        when(analysis.getStatus())
                .thenReturn(AnalysisStatus.COMPLETED);

        when(analysis.getMaskKey())
                .thenReturn("predicted.png");

        when(analyses.findByIdAndImage_User_Id(
                analysisId, ownerId
        )).thenReturn(Optional.of(analysis));

        when(analyses.findOwnedForEvaluationUpdate(
                analysisId, ownerId
        )).thenReturn(Optional.of(analysis));

        when(storage.read("predicted.png"))
                .thenReturn(new byte[]{4, 5, 6});

        when(evaluation.evaluate(any(), any()))
                .thenReturn(
                        new com.medvision.segmentation.evaluation
                                .SegmentationMetrics.Result(
                                        1.0, 1.0, 4, 0, 0
                                )
                );

        when(storage.store(any(), eq("png")))
                .thenReturn("ground-truth.png");

        when(analyses.saveAndFlush(any(Analysis.class)))
                .thenThrow(new IllegalStateException("Database failure"));

        assertThrows(
                IllegalStateException.class,
                () -> service().evaluate(ownerId, analysisId, upload)
        );

        verify(storage).delete("ground-truth.png");
    }
    @Test
    void rejectsGroundTruthWithDifferentDimensions() throws java.io.IOException {
        var upload = file();
        validUpload(upload);

        var analysis = mock(Analysis.class);

        when(analysis.getStatus())
                .thenReturn(AnalysisStatus.COMPLETED);

        when(analysis.getMaskKey())
                .thenReturn("predicted.png");

        when(analyses.findByIdAndImage_User_Id(
                analysisId, ownerId
        )).thenReturn(Optional.of(analysis));

        when(storage.read("predicted.png"))
                .thenReturn(new byte[]{4, 5, 6});

        when(evaluation.evaluate(any(), any()))
                .thenThrow(new IllegalArgumentException(
                        "Prediction and ground-truth dimensions must match."
                ));

        var ex = assertThrows(
                ApiException.class,
                () -> service().evaluate(ownerId, analysisId, upload)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(
                "GROUND_TRUTH_DIMENSIONS_MISMATCH",
                ex.getCode()
        );

        verify(storage, never()).store(any(), any());
        verify(analyses, never())
                .findOwnedForEvaluationUpdate(any(), any());
    }
    @Test
    void successfullyEvaluatesCompletedAnalysis() throws java.io.IOException {
        var upload = file();
        validUpload(upload);

        var image = mock(com.medvision.image.MedicalImage.class);
        when(image.getWidth()).thenReturn(2);
        when(image.getHeight()).thenReturn(2);

        var analysis = new Analysis(
                image,
                "OTSU",
                "Evaluation success test",
                java.util.Map.of()
        );

        analysis.complete(
                "predicted.png",
                "overlay.png",
                2,
                0.5,
                15
        );

        when(analyses.findByIdAndImage_User_Id(
                analysisId, ownerId
        )).thenReturn(Optional.of(analysis));

        when(analyses.findOwnedForEvaluationUpdate(
                analysisId, ownerId
        )).thenReturn(Optional.of(analysis));

        byte[] prediction = new byte[]{4, 5, 6};

        when(storage.read("predicted.png"))
                .thenReturn(prediction);

        when(evaluation.evaluate(any(), any()))
                .thenReturn(
                        new com.medvision.segmentation.evaluation
                                .SegmentationMetrics.Result(
                                        0.8, 2.0 / 3.0, 2, 1, 0
                                )
                );

        when(storage.store(any(), eq("png")))
                .thenReturn("ground-truth.png");

        when(analyses.saveAndFlush(any(Analysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnalysisResponse response = service().evaluate(
                ownerId,
                analysisId,
                upload
        );

        assertNotNull(response);
        assertEquals(0.8, response.diceScore(), 1e-9);
        assertEquals(2.0 / 3.0, response.iouScore(), 1e-9);
        assertEquals(Boolean.TRUE, response.hasGroundTruth());

        assertEquals("ground-truth.png", analysis.getGroundTruthKey());

        verify(storage).read("predicted.png");
        verify(storage).store(any(), eq("png"));
        verify(analyses).saveAndFlush(analysis);
        verify(storage, never()).delete(any());
    }
}
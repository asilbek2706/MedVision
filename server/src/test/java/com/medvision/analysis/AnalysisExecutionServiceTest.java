package com.medvision.analysis;

import com.medvision.common.error.ApiException;
import com.medvision.image.MedicalImage;
import com.medvision.image.MedicalImageRepository;
import com.medvision.segmentation.SegmentationService;
import com.medvision.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisExecutionServiceTest {

    @TempDir
    Path directory;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID imageId = UUID.randomUUID();

    private byte[] samplePng() {
        nu.pattern.OpenCV.loadLocally();

        Mat image = new Mat(
                100, 100, CvType.CV_8UC3,
                new Scalar(0, 0, 0)
        );

        MatOfByte encoded = new MatOfByte();

        try {
            Imgproc.rectangle(
                    image,
                    new Point(20, 20),
                    new Point(79, 79),
                    new Scalar(255, 255, 255),
                    -1
            );

            assertTrue(
                    Imgcodecs.imencode(".png", image, encoded)
            );

            return encoded.toArray();
        } finally {
            encoded.release();
            image.release();
        }
    }

    private PlatformTransactionManager transactionManager() {
        var manager = mock(PlatformTransactionManager.class);

        when(manager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());

        return manager;
    }

    private MedicalImage image(String key, long size) {
        var image = mock(MedicalImage.class);

        when(image.getStorageKey()).thenReturn(key);
        when(image.getSizeBytes()).thenReturn(size);
        when(image.getWidth()).thenReturn(100);
        when(image.getHeight()).thenReturn(100);
        when(image.getId()).thenReturn(imageId);

        return image;
    }

    private AnalysisExecutionService service(
            MedicalImageRepository images,
            AnalysisRepository analyses,
            StorageService storage,
            PlatformTransactionManager manager
    ) {
        return new AnalysisExecutionService(
                images,
                analyses,
                storage,
                new SegmentationService(),
                manager
        );
    }

    private AnalysisParametersRequest parameters() {
        return new AnalysisParametersRequest(
                "OTSU", 5, false
        );
    }

    private long fileCount() throws Exception {
        try (var files = Files.list(directory)) {
            return files.count();
        }
    }

    @Test
    void successfullyStoresMaskAndOverlay() throws Exception {
        byte[] png = samplePng();

        var storage = new StorageService(directory.toString());
        String originalKey = storage.store(png, "png");

        var images = mock(MedicalImageRepository.class);
        var analyses = mock(AnalysisRepository.class);
        var manager = transactionManager();

        MedicalImage source = image(originalKey, png.length);

        when(images.findByIdAndUser_Id(imageId, ownerId))
                .thenReturn(Optional.of(source));

        when(analyses.saveAndFlush(any(Analysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var execution = service(
                images, analyses, storage, manager
        );

        // Mock entity has no generated ID, but execution should
        // still complete and persist the correct statistics.
        AnalysisResponse response = execution.execute(
                ownerId, imageId, parameters()
        );

        assertEquals(AnalysisStatus.COMPLETED, response.status());
        assertEquals("OTSU", response.algorithm());

        assertNotNull(response.foregroundPixels());
        assertTrue(response.foregroundPixels() > 0);
        assertTrue(response.foregroundRatio() > 0);
        assertTrue(response.foregroundRatio() < 1);

        assertEquals(3, fileCount());

        verify(analyses).saveAndFlush(argThat(analysis ->
                analysis.getStatus() == AnalysisStatus.COMPLETED
                        && analysis.getMaskKey() != null
                        && analysis.getOverlayKey() != null
                        && analysis.getProcessingTimeMs() >= 0
        ));
    }

    @Test
    void rejectsImagesOwnedByAnotherUser() throws Exception {
        var storage = new StorageService(directory.toString());

        var images = mock(MedicalImageRepository.class);
        var analyses = mock(AnalysisRepository.class);

        when(images.findByIdAndUser_Id(imageId, ownerId))
                .thenReturn(Optional.empty());

        var execution = service(
                images,
                analyses,
                storage,
                transactionManager()
        );

        ApiException error = assertThrows(
                ApiException.class,
                () -> execution.execute(
                        ownerId, imageId, parameters()
                )
        );

        assertEquals(404, error.getStatus().value());
        assertEquals("NOT_FOUND", error.getCode());

        verifyNoInteractions(analyses);
        assertEquals(0, fileCount());
    }

    @Test
    void databaseFailureRemovesGeneratedFiles() throws Exception {
        byte[] png = samplePng();

        var storage = new StorageService(directory.toString());
        String originalKey = storage.store(png, "png");

        var images = mock(MedicalImageRepository.class);
        var analyses = mock(AnalysisRepository.class);
        var manager = transactionManager();

        MedicalImage source = image(originalKey, png.length);

        when(images.findByIdAndUser_Id(imageId, ownerId))
                .thenReturn(Optional.of(source));

        when(analyses.saveAndFlush(any(Analysis.class)))
                .thenThrow(
                        new IllegalStateException(
                                "Simulated database failure"
                        )
                );

        var execution = service(
                images, analyses, storage, manager
        );

        assertThrows(
                IllegalStateException.class,
                () -> execution.execute(
                        ownerId, imageId, parameters()
                )
        );

        // Only the original image should remain.
        assertEquals(1, fileCount());
        assertArrayEquals(png, storage.read(originalKey));

        verify(manager).rollback(any());
    }
}
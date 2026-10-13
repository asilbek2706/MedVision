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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisExecutionServiceTest {

    @TempDir
    Path directory;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID imageId = UUID.randomUUID();
    private final UUID analysisId = UUID.randomUUID();

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

            assertTrue(Imgcodecs.imencode(".png", image, encoded));
            return encoded.toArray();
        } finally {
            encoded.release();
            image.release();
        }
    }

    private PlatformTransactionManager transactionManager() {
        var manager = mock(PlatformTransactionManager.class);

        when(manager.getTransaction(any()))
                .thenAnswer(invocation -> new SimpleTransactionStatus());

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
        return new AnalysisParametersRequest("OTSU", 5, false);
    }

    private long fileCount() throws Exception {
        try (var files = Files.list(directory)) {
            return files.count();
        }
    }

    private AtomicReference<Analysis> configureRepository(
            AnalysisRepository analyses
    ) {
        var saved = new AtomicReference<Analysis>();

        when(analyses.saveAndFlush(any(Analysis.class)))
                .thenAnswer(invocation -> {
                    Analysis analysis = invocation.getArgument(0);
                    saved.set(analysis);

                    var idField = Analysis.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(analysis, analysisId);

                    return analysis;
                });

        when(analyses.findById(analysisId))
                .thenAnswer(invocation -> Optional.ofNullable(saved.get()));

        return saved;
    }

    @Test
    void successfullyTransitionsProcessingToCompleted() throws Exception {
        byte[] png = samplePng();

        var storage = new StorageService(directory.toString());
        String originalKey = storage.store(png, "png");

        var images = mock(MedicalImageRepository.class);
        var analyses = mock(AnalysisRepository.class);
        var manager = transactionManager();

        MedicalImage source = image(originalKey, png.length);

        when(images.findByIdAndUser_Id(imageId, ownerId))
                .thenReturn(Optional.of(source));

        configureRepository(analyses);

        var execution = service(images, analyses, storage, manager);

        AnalysisResponse response = execution.execute(
                ownerId, imageId, parameters()
        );

        assertEquals(AnalysisStatus.COMPLETED, response.status());
        assertEquals(analysisId, response.id());
        assertNotNull(response.foregroundPixels());
        assertTrue(response.foregroundPixels() > 0);
        assertTrue(response.foregroundRatio() > 0);
        assertTrue(response.foregroundRatio() < 1);

        assertEquals(3, fileCount());

        verify(analyses, times(2)).saveAndFlush(any(Analysis.class));
        verify(analyses).findById(analysisId);
    }

    @Test
    void rejectsImagesOwnedByAnotherUser() throws Exception {
        var storage = new StorageService(directory.toString());

        var images = mock(MedicalImageRepository.class);
        var analyses = mock(AnalysisRepository.class);

        when(images.findByIdAndUser_Id(imageId, ownerId))
                .thenReturn(Optional.empty());

        var execution = service(
                images, analyses, storage, transactionManager()
        );

        ApiException error = assertThrows(
                ApiException.class,
                () -> execution.execute(ownerId, imageId, parameters())
        );

        assertEquals(404, error.getStatus().value());
        assertEquals("NOT_FOUND", error.getCode());

        verifyNoInteractions(analyses);
        assertEquals(0, fileCount());
    }

    @Test
    void segmentationFailureTransitionsToFailed() throws Exception {
        var storage = new StorageService(directory.toString());

        var images = mock(MedicalImageRepository.class);
        var analyses = mock(AnalysisRepository.class);

        MedicalImage source = image("missing.png", 100);

        when(images.findByIdAndUser_Id(imageId, ownerId))
                .thenReturn(Optional.of(source));

        AtomicReference<Analysis> saved = configureRepository(analyses);

        var execution = service(
                images, analyses, storage, transactionManager()
        );

        assertThrows(
                RuntimeException.class,
                () -> execution.execute(ownerId, imageId, parameters())
        );

        assertNotNull(saved.get());
        assertEquals(AnalysisStatus.FAILED, saved.get().getStatus());
        assertEquals("SEGMENTATION_FAILED", saved.get().getErrorCode());
        assertEquals(0, fileCount());
    }

    @Test
    void completionFailureRemovesGeneratedFiles() throws Exception {
        byte[] png = samplePng();

        var storage = new StorageService(directory.toString());
        String originalKey = storage.store(png, "png");

        var images = mock(MedicalImageRepository.class);
        var analyses = mock(AnalysisRepository.class);

        MedicalImage source = image(originalKey, png.length);

        when(images.findByIdAndUser_Id(imageId, ownerId))
                .thenReturn(Optional.of(source));

        AtomicReference<Analysis> saved = configureRepository(analyses);

        when(analyses.findById(analysisId))
                .thenAnswer(invocation -> {
                    Analysis analysis = saved.get();

                    if (analysis.getStatus() == AnalysisStatus.PROCESSING) {
                        throw new IllegalStateException(
                                "Simulated completion failure"
                        );
                    }

                    return Optional.of(analysis);
                });

        var execution = service(
                images, analyses, storage, transactionManager()
        );

        assertThrows(
                IllegalStateException.class,
                () -> execution.execute(ownerId, imageId, parameters())
        );

        assertEquals(1, fileCount());
        assertArrayEquals(png, storage.read(originalKey));
    }
}
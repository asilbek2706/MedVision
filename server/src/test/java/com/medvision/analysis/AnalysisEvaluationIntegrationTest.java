package com.medvision.analysis;

import com.medvision.auth.User;
import com.medvision.auth.UserRepository;
import com.medvision.image.MedicalImage;
import com.medvision.image.MedicalImageRepository;
import com.medvision.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AnalysisEvaluationIntegrationTest {

    private static final Path ROOT = createRoot();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("medvision.storage.root", ROOT::toString);
        registry.add("medvision.storage.cleanup.enabled", () -> "false");
    }

    @Autowired private AnalysisExecutionService execution;
    @Autowired private AnalysisEvaluationExecutionService evaluation;
    @Autowired private AnalysisRepository analyses;
    @Autowired private MedicalImageRepository images;
    @Autowired private UserRepository users;
    @Autowired private StorageService storage;
    @Autowired private JdbcTemplate jdbc;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> imageIds = new ArrayList<>();
    private final List<String> storageKeys = new ArrayList<>();

    @Test
    void evaluatesRealSegmentationWithPerfectGroundTruth() throws Exception {
        User owner = users.saveAndFlush(new User(
                "Evaluation Integration Test",
                UUID.randomUUID() + "@example.test",
                "test-only-hash"
        ));

        userIds.add(owner.getId());

        byte[] originalPng = png();

        String originalKey = storage.store(originalPng, "png");
        storageKeys.add(originalKey);

        MedicalImage image = images.saveAndFlush(new MedicalImage(
                owner,
                "evaluation-source.png",
                originalKey,
                "image/png",
                originalPng.length,
                100,
                100
        ));

        imageIds.add(image.getId());

        AnalysisResponse completed = execution.execute(
                owner.getId(),
                image.getId(),
                new AnalysisParametersRequest(
                        "Evaluation integration",
                        "OTSU",
                        5,
                        false
                )
        );

        assertEquals(AnalysisStatus.COMPLETED, completed.status());

        Analysis analysis = analyses.findById(
                completed.id()
        ).orElseThrow();

        storageKeys.add(analysis.getMaskKey());
        storageKeys.add(analysis.getOverlayKey());

        byte[] predictedMask = storage.read(
                analysis.getMaskKey()
        );

        var upload = new MockMultipartFile(
                "file",
                "ground-truth.png",
                "image/png",
                predictedMask
        );

        AnalysisResponse result = evaluation.evaluate(
                owner.getId(),
                completed.id(),
                upload
        );

        assertEquals(1.0, result.diceScore(), 1e-9);
        assertEquals(1.0, result.iouScore(), 1e-9);
        assertEquals(Boolean.TRUE, result.hasGroundTruth());

        Analysis persisted = analyses.findById(
                completed.id()
        ).orElseThrow();

        assertEquals(1.0, persisted.getDiceScore(), 1e-9);
        assertEquals(1.0, persisted.getIouScore(), 1e-9);
        assertNotNull(persisted.getGroundTruthKey());

        storageKeys.add(persisted.getGroundTruthKey());

        assertArrayEquals(
                predictedMask,
                storage.read(persisted.getGroundTruthKey())
        );

        assertTrue(
                Files.exists(ROOT.resolve(persisted.getGroundTruthKey()))
        );
    }

    @Test
    void rejectsRepeatedEvaluation() throws Exception {
        var fixture = completedAnalysis();

        byte[] mask = storage.read(fixture.analysis().getMaskKey());

        var file = new MockMultipartFile(
                "file", "ground-truth.png", "image/png", mask
        );

        evaluation.evaluate(
                fixture.ownerId(), fixture.analysis().getId(), file
        );

        var error = assertThrows(
                com.medvision.common.error.ApiException.class,
                () -> evaluation.evaluate(
                        fixture.ownerId(),
                        fixture.analysis().getId(),
                        file
                )
        );

        assertEquals(
                org.springframework.http.HttpStatus.CONFLICT,
                error.getStatus()
        );
    }

    @Test
    void rejectsGroundTruthWithDifferentDimensions() throws Exception {
        var fixture = completedAnalysis();

        byte[] wrongSize = createPng(20, 20);

        var file = new MockMultipartFile(
                "file", "ground-truth.png", "image/png", wrongSize
        );

        var error = assertThrows(
                com.medvision.common.error.ApiException.class,
                () -> evaluation.evaluate(
                        fixture.ownerId(),
                        fixture.analysis().getId(),
                        file
                )
        );

        assertEquals(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                error.getStatus()
        );

        Analysis persisted = analyses.findById(
                fixture.analysis().getId()
        ).orElseThrow();

        assertNull(persisted.getGroundTruthKey());
        assertNull(persisted.getDiceScore());
        assertNull(persisted.getIouScore());
    }

    private Fixture completedAnalysis() throws Exception {
        User owner = users.saveAndFlush(new User(
                "Evaluation Edge Case",
                UUID.randomUUID() + "@example.test",
                "test-only-hash"
        ));

        userIds.add(owner.getId());

        byte[] source = png();

        String sourceKey = storage.store(source, "png");
        storageKeys.add(sourceKey);

        MedicalImage image = images.saveAndFlush(new MedicalImage(
                owner,
                "edge-case.png",
                sourceKey,
                "image/png",
                source.length,
                100,
                100
        ));

        imageIds.add(image.getId());

        AnalysisResponse completed = execution.execute(
                owner.getId(),
                image.getId(),
                new AnalysisParametersRequest(
                        "Edge case evaluation",
                        "OTSU",
                        5,
                        false
                )
        );

        Analysis analysis = analyses.findById(
                completed.id()
        ).orElseThrow();

        storageKeys.add(analysis.getMaskKey());
        storageKeys.add(analysis.getOverlayKey());

        return new Fixture(owner.getId(), analysis);
    }

    private static byte[] createPng(
            int width,
            int height
    ) throws Exception {
        var image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_BYTE_GRAY
        );

        var graphics = image.createGraphics();

        try {
            graphics.setColor(java.awt.Color.BLACK);
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(
                    width / 4,
                    height / 4,
                    width / 2,
                    height / 2
            );
        } finally {
            graphics.dispose();
        }

        try (var output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, "png", output));
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }

    private record Fixture(UUID ownerId, Analysis analysis) {}
    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(
                100,
                100,
                BufferedImage.TYPE_INT_RGB
        );

        var graphics = image.createGraphics();

        try {
            graphics.setColor(java.awt.Color.BLACK);
            graphics.fillRect(0, 0, 100, 100);
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(20, 20, 60, 60);
        } finally {
            graphics.dispose();
        }

        try (var output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, "png", output));
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }

    @AfterEach
    void cleanup() {
        for (UUID imageId : imageIds) {
            List<Analysis> saved = analyses.findAll().stream()
                    .filter(a -> a.getImage().getId().equals(imageId))
                    .toList();

            for (Analysis analysis : saved) {
                if (analysis.getMaskKey() != null) {
                    storageKeys.add(analysis.getMaskKey());
                }

                if (analysis.getOverlayKey() != null) {
                    storageKeys.add(analysis.getOverlayKey());
                }

                if (analysis.getGroundTruthKey() != null) {
                    storageKeys.add(analysis.getGroundTruthKey());
                }
            }

            jdbc.update("DELETE FROM analyses WHERE image_id = ?", imageId);
            jdbc.update("DELETE FROM medical_images WHERE id = ?", imageId);
        }

        for (UUID userId : userIds) {
            jdbc.update("DELETE FROM users WHERE id = ?", userId);
        }

        for (String key : storageKeys) {
            storage.delete(key);
        }
    }

    private static Path createRoot() {
        try {
            return Files.createTempDirectory(
                    "medvision-evaluation-integration-"
            );
        } catch (java.io.IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
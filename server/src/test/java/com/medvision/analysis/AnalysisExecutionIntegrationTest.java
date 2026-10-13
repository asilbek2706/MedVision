package com.medvision.analysis;

import com.medvision.auth.User;
import com.medvision.auth.UserRepository;
import com.medvision.common.error.ApiException;
import com.medvision.image.MedicalImage;
import com.medvision.image.MedicalImageRepository;
import com.medvision.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AnalysisExecutionIntegrationTest {

    private static final Path ROOT = createRoot();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("medvision.storage.root", ROOT::toString);
        registry.add("medvision.storage.cleanup.enabled", () -> "false");
    }

    @Autowired private AnalysisExecutionService execution;
    @Autowired private AnalysisRepository analyses;
    @Autowired private MedicalImageRepository images;
    @Autowired private UserRepository users;
    @Autowired private StorageService storage;
    @Autowired private JdbcTemplate jdbc;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> imageIds = new ArrayList<>();
    private final List<String> storageKeys = new ArrayList<>();

    private User createUser() {
        User user = users.saveAndFlush(new User(
                "Analysis Integration Test",
                UUID.randomUUID() + "@example.test",
                "test-only-hash"
        ));

        userIds.add(user.getId());
        return user;
    }

    private MedicalImage createImage(User owner, byte[] bytes) {
        String key = storage.store(bytes, "png");
        storageKeys.add(key);

        MedicalImage image = images.saveAndFlush(new MedicalImage(
                owner,
                "test.png",
                key,
                "image/png",
                bytes.length,
                100,
                100
        ));

        imageIds.add(image.getId());
        return image;
    }

    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(
                100, 100, BufferedImage.TYPE_INT_RGB
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

    private AnalysisParametersRequest parameters() {
        return new AnalysisParametersRequest("OTSU", 5, false);
    }

    @Test
    void completedAnalysisPersistsAcrossTransactions() throws Exception {
        User owner = createUser();
        MedicalImage image = createImage(owner, png());

        AnalysisResponse response = execution.execute(
                owner.getId(),
                image.getId(),
                parameters()
        );

        assertEquals(AnalysisStatus.COMPLETED, response.status());

        Analysis stored = analyses.findById(response.id()).orElseThrow();

        assertEquals(AnalysisStatus.COMPLETED, stored.getStatus());
        assertNotNull(stored.getCompletedAt());
        assertNotNull(stored.getMaskKey());
        assertNotNull(stored.getOverlayKey());
        assertTrue(stored.getForegroundPixels() > 0);

        storageKeys.add(stored.getMaskKey());
        storageKeys.add(stored.getOverlayKey());

        assertTrue(Files.exists(ROOT.resolve(stored.getMaskKey())));
        assertTrue(Files.exists(ROOT.resolve(stored.getOverlayKey())));
    }

    @Test
    void invalidImagePersistsFailedStatus() {
        User owner = createUser();
        MedicalImage image = createImage(
                owner,
                "not-a-real-png".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        assertThrows(
                RuntimeException.class,
                () -> execution.execute(
                        owner.getId(),
                        image.getId(),
                        parameters()
                )
        );

        List<Analysis> saved = analyses.findAll().stream()
                .filter(a -> a.getImage().getId().equals(image.getId()))
                .toList();

        assertEquals(1, saved.size());
        assertEquals(AnalysisStatus.FAILED, saved.get(0).getStatus());
        assertEquals("SEGMENTATION_FAILED", saved.get(0).getErrorCode());
        assertNotNull(saved.get(0).getCompletedAt());
        assertNull(saved.get(0).getMaskKey());
        assertNull(saved.get(0).getOverlayKey());
    }

    @Test
    void rejectsAnotherUsersImageWithoutCreatingAnalysis() throws Exception {
        User owner = createUser();
        User other = createUser();
        MedicalImage image = createImage(owner, png());

        ApiException error = assertThrows(
                ApiException.class,
                () -> execution.execute(
                        other.getId(),
                        image.getId(),
                        parameters()
                )
        );

        assertEquals(404, error.getStatus().value());

        assertTrue(analyses.findAll().stream()
                .noneMatch(a -> a.getImage().getId().equals(image.getId())));
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
            return Files.createTempDirectory("medvision-analysis-integration-");
        } catch (java.io.IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
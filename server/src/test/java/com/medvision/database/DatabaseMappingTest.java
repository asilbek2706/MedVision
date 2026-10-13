package com.medvision.database;

import com.medvision.auth.*;
import com.medvision.image.*;
import com.medvision.analysis.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseMappingTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private MedicalImageRepository images;

    @Autowired
    private AnalysisRepository analyses;

    @Autowired
    private EntityManager entityManager;

    private User createUser() {
        return users.saveAndFlush(new User(
                "Test User",
                UUID.randomUUID() + "@example.test",
                "test-only-hash"
        ));
    }

    private MedicalImage createImage(User user) {
        return images.saveAndFlush(new MedicalImage(
                user,
                "sample.png",
                "images/" + UUID.randomUUID() + ".png",
                "image/png",
                1024,
                100,
                100
        ));
    }

    @Test
    void savesAndReloadsRelationshipsAndJson() {
        User owner = createUser();
        MedicalImage image = createImage(owner);
        Analysis analysis = analyses.saveAndFlush(new Analysis(
                image,
                "OTSU",
                "Test tahlil", Map.of("blurKernelSize", 5, "invert", false)
        ));

        UUID ownerId = owner.getId();
        UUID imageId = image.getId();
        UUID analysisId = analysis.getId();

        entityManager.clear();

        Analysis loaded = analyses.findById(analysisId).orElseThrow();

        assertEquals(imageId, loaded.getImage().getId());
        assertEquals(ownerId, loaded.getImage().getUser().getId());
        assertEquals(5, ((Number) loaded.getParameters()
                .get("blurKernelSize")).intValue());
        assertEquals(false, loaded.getParameters().get("invert"));
        assertEquals(AnalysisStatus.PROCESSING, loaded.getStatus());
        assertNotNull(loaded.getCreatedAt());
        assertNull(loaded.getCompletedAt());
    }

    @Test
    void ownerQueriesExcludeAnotherUser() {
        User owner = createUser();
        User other = createUser();
        MedicalImage image = createImage(owner);
        Analysis analysis = analyses.saveAndFlush(new Analysis(
                image, "OTSU", "Test tahlil", Map.of()
        ));

        assertTrue(images.findByIdAndUser_Id(
                image.getId(), owner.getId()).isPresent());
        assertTrue(images.findByIdAndUser_Id(
                image.getId(), other.getId()).isEmpty());

        assertTrue(analyses.findByIdAndImage_User_Id(
                analysis.getId(), owner.getId()).isPresent());
        assertTrue(analyses.findByIdAndImage_User_Id(
                analysis.getId(), other.getId()).isEmpty());
    }

    @Test
    void rejectsDuplicateNormalizedEmail() {
        String email = UUID.randomUUID() + "@example.test";

        users.saveAndFlush(new User("First", email, "test-only-hash"));

        assertThrows(DataIntegrityViolationException.class, () ->
                users.saveAndFlush(new User(
                        "Second",
                        "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ",
                        "test-only-hash"
                ))
        );
    }

    @Test
    void preventsDeletingImageWithAnalysis() {
        MedicalImage image = createImage(createUser());
        analyses.saveAndFlush(new Analysis(image, "OTSU", "Test tahlil", Map.of()));

        assertThrows(DataIntegrityViolationException.class, () -> {
            UUID imageId = image.getId();
            entityManager.clear();
            images.deleteById(imageId);
            images.flush();
        });
    }

    @Test
    void preventsDeletingUserWithImage() {
        User owner = createUser();
        createImage(owner);

        assertThrows(DataIntegrityViolationException.class, () -> {
            UUID ownerId = owner.getId();
            entityManager.clear();
            users.deleteById(ownerId);
            users.flush();
        });
    }

    @Test
    void persistsCompletedAnalysisAndPreventsSecondFinalization() {
        MedicalImage image = createImage(createUser());
        Analysis analysis = analyses.saveAndFlush(
                new Analysis(image, "OTSU", "Test tahlil", Map.of())
        );

        analysis.complete("masks/test.png", "overlays/test.png",
                2500, 0.25, 120);

        analyses.flush();
        UUID id = analysis.getId();
        entityManager.clear();

        Analysis loaded = analyses.findById(id).orElseThrow();

        assertEquals(AnalysisStatus.COMPLETED, loaded.getStatus());
        assertEquals(2500L, loaded.getForegroundPixels().longValue());
        assertEquals(0.25, loaded.getForegroundRatio().doubleValue(), 0.000001);
        assertNotNull(loaded.getCompletedAt());
        assertThrows(IllegalStateException.class,
                () -> loaded.fail("SECOND_FINALIZATION"));
    }

    @Test
    void persistsFailedAnalysis() {
        Analysis analysis = analyses.saveAndFlush(new Analysis(
                createImage(createUser()), "OTSU", "Test tahlil", Map.of()
        ));

        analysis.fail("PROCESSING_FAILED");
        analyses.flush();

        UUID id = analysis.getId();
        entityManager.clear();

        Analysis loaded = analyses.findById(id).orElseThrow();

        assertEquals(AnalysisStatus.FAILED, loaded.getStatus());
        assertEquals("PROCESSING_FAILED", loaded.getErrorCode());
        assertNotNull(loaded.getCompletedAt());
        assertNull(loaded.getMaskKey());
        assertNull(loaded.getOverlayKey());
    }
}
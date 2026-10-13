package com.medvision.image;

import com.medvision.auth.*;
import com.medvision.analysis.*;
import com.medvision.common.error.ApiException;
import com.medvision.storage.StorageCleanupService;
import org.junit.jupiter.api.*;
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
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ImageStorageIntegrationTest {

    private static final Path ROOT = temporaryRoot();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("medvision.storage.root", ROOT::toString);
        registry.add("medvision.storage.cleanup.enabled", () -> "false");
    }

    @Autowired private ImageFileService files;
    @Autowired private ImageDeletionService deletion;
    @Autowired private StorageCleanupService cleanup;
    @Autowired private UserRepository users;
    @Autowired private MedicalImageRepository images;
    @Autowired private AnalysisRepository analyses;
    @Autowired private JdbcTemplate jdbc;

    private final List<UUID> userIds = new ArrayList<>();
    private final List<UUID> imageIds = new ArrayList<>();
    private final List<String> keys = new ArrayList<>();

    @Test
    void pngAndJpegLifecycleEnforcesOwnership() throws Exception {
        User a = user();
        User b = user();

        for (String format : new String[]{"png", "jpeg"}) {
            MockMultipartFile upload = image(format);
            var response = files.upload(a.getId(), upload);
            var stored = track(response.id());

            assertEquals(8, response.width());
            assertEquals(6, response.height());
            assertTrue(Files.exists(ROOT.resolve(stored.getStorageKey())));
            assertArrayEquals(
                    upload.getBytes(),
                    files.content(a.getId(), response.id()).bytes()
            );

            assertStatus(404, () -> files.content(b.getId(), response.id()));
            assertStatus(404, () -> deletion.delete(b.getId(), response.id()));

            assertTrue(images.existsById(response.id()));
            assertTrue(Files.exists(ROOT.resolve(stored.getStorageKey())));

            deletion.delete(a.getId(), response.id());

            assertFalse(images.existsById(response.id()));
            assertFalse(Files.exists(ROOT.resolve(stored.getStorageKey())));
            assertEquals(0L, queued(stored.getStorageKey()));
            assertStatus(404, () -> files.content(a.getId(), response.id()));
        }
    }

    @Test
    void imageWithAnalysisIsNotDeleted() throws Exception {
        User owner = user();
        var response = files.upload(owner.getId(), image("png"));
        var stored = track(response.id());

        analyses.saveAndFlush(new Analysis(stored, "OTSU", Map.of()));

        ApiException error = assertThrows(
                ApiException.class,
                () -> deletion.delete(owner.getId(), response.id())
        );

        assertEquals(409, error.getStatus().value());
        assertEquals("IMAGE_HAS_ANALYSES", error.getCode());
        assertTrue(images.existsById(response.id()));
        assertTrue(Files.exists(ROOT.resolve(stored.getStorageKey())));
        assertEquals(0L, queued(stored.getStorageKey()));
    }

    @Test
    void failedFileCleanupRemainsQueuedAndCanRetry() throws Exception {
        User owner = user();
        var response = files.upload(owner.getId(), image("png"));
        var stored = track(response.id());
        String key = stored.getStorageKey();
        Path target = ROOT.resolve(key);

        // A nonempty directory makes physical deletion fail reliably.
        Files.delete(target);
        Files.createDirectory(target);
        Path blocker = Files.write(target.resolve("blocker"), new byte[]{1});

        deletion.delete(owner.getId(), response.id());

        assertFalse(images.existsById(response.id()));
        assertEquals(1L, queued(key));
        assertEquals(1L, jdbc.queryForObject(
                "SELECT attempts FROM storage_deletion_jobs WHERE storage_key = ?",
                Long.class, key
        ));
        assertStatus(404, () -> files.content(owner.getId(), response.id()));

        Files.delete(blocker);

        jdbc.update("""
                UPDATE storage_deletion_jobs
                SET next_retry_at = CURRENT_TIMESTAMP
                WHERE storage_key = ?
                """, key);

        cleanup.processPending();

        assertEquals(0L, queued(key));
        assertFalse(Files.exists(target));
    }

    private User user() {
        User user = users.saveAndFlush(new User(
                "Storage Test",
                UUID.randomUUID() + "@example.test",
                "test-only-hash"
        ));
        userIds.add(user.getId());
        return user;
    }

    private MedicalImage track(UUID id) {
        imageIds.add(id);
        MedicalImage image = images.findById(id).orElseThrow();
        keys.add(image.getStorageKey());
        return image;
    }

    private long queued(String key) {
        return Objects.requireNonNull(jdbc.queryForObject(
                "SELECT count(*) FROM storage_deletion_jobs WHERE storage_key = ?",
                Long.class, key
        ));
    }

    private void assertStatus(int status, Runnable action) {
        ApiException error = assertThrows(ApiException.class, action::run);
        assertEquals(status, error.getStatus().value());
    }

    private MockMultipartFile image(String format) throws Exception {
        var image = new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB);
        try (var output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, format, output));
            return new MockMultipartFile(
                    "file", "scan." + format,
                    "png".equals(format) ? "image/png" : "image/jpeg",
                    output.toByteArray()
            );
        } finally {
            image.flush();
        }
    }

    @AfterEach
    void removeOnlyThisTestsRows() {
        for (UUID id : imageIds) {
            jdbc.update("DELETE FROM analyses WHERE image_id = ?", id);
            jdbc.update("DELETE FROM medical_images WHERE id = ?", id);
        }
        for (String key : keys) {
            jdbc.update("DELETE FROM storage_deletion_jobs WHERE storage_key = ?", key);
        }
        for (UUID id : userIds) {
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
    }

    @AfterAll
    static void removeTestDirectory() throws Exception {
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static Path temporaryRoot() {
        try {
            return Files.createTempDirectory("medvision-storage-integration-");
        } catch (java.io.IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}

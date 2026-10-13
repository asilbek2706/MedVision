package com.medvision.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    @TempDir
    Path directory;

    @Test
    void storesWithUniqueUuidNamesAndReadsExactBytes() throws Exception {
        StorageService storage = new StorageService(directory.toString());
        byte[] bytes = {1, 2, 3, 4};

        String first = storage.store(bytes, "png");
        String second = storage.store(bytes, "png");

        assertTrue(first.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png"
        ));
        assertNotEquals(first, second);
        assertArrayEquals(bytes, storage.read(first));
        assertArrayEquals(bytes, storage.read(second));

        storage.delete(first);
        assertFalse(Files.exists(directory.resolve(first)));
        assertTrue(Files.exists(directory.resolve(second)));

        // Repeating cleanup is safe.
        assertDoesNotThrow(() -> storage.delete(first));
    }

    @Test
    void rejectsTraversalAndArbitraryStorageKeys() {
        StorageService storage = new StorageService(directory.toString());

        for (String key : new String[]{
                "../secret.png",
                "/etc/passwd",
                "..\\secret.png",
                "original-name.png",
                "00000000-0000-0000-0000-000000000000.svg"
        }) {
            assertThrows(IllegalArgumentException.class, () -> storage.read(key));
            assertThrows(IllegalArgumentException.class, () -> storage.delete(key));
        }
    }

    @Test
    void refusesToReadSymbolicLinks() throws Exception {
        StorageService storage = new StorageService(directory.toString());

        Path outside = Files.createTempFile("medvision-outside-", ".png");
        String key = "00000000-0000-0000-0000-000000000000.png";

        try {
            Files.write(outside, new byte[]{1, 2, 3});
            Files.createSymbolicLink(directory.resolve(key), outside);

            assertThrows(IllegalStateException.class, () -> storage.read(key));
            assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(outside));

            storage.delete(key);
            assertTrue(Files.exists(outside));
        } finally {
            Files.deleteIfExists(directory.resolve(key));
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void rejectsInvalidStorageInput() {
        StorageService storage = new StorageService(directory.toString());

        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new byte[0], "png"));

        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new byte[]{1}, "../png"));

        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new byte[10 * 1024 * 1024 + 1], "png"));
    }
}
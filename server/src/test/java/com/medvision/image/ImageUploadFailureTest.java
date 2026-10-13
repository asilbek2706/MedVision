package com.medvision.image;

import com.medvision.auth.*;
import com.medvision.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageUploadFailureTest {

    @TempDir
    Path directory;

    @Test
    void databaseWriteFailureRemovesStoredFile() throws Exception {
        var images = mock(MedicalImageRepository.class);
        var users = mock(UserRepository.class);
        var manager = manager();
        var storage = new StorageService(directory.toString());
        UUID ownerId = UUID.randomUUID();

        when(users.findById(ownerId)).thenReturn(Optional.of(
                new User("Test", "test@example.test", "test-hash")
        ));
        when(images.saveAndFlush(any(MedicalImage.class)))
                .thenThrow(new IllegalStateException("Simulated DB failure"));

        var service = new ImageFileService(
                new ImageUploadValidator(), storage, images, users, manager
        );

        assertThrows(IllegalStateException.class,
                () -> service.upload(ownerId, png()));
        assertDirectoryEmpty();
        verify(manager).rollback(any());
    }

    @Test
    void databaseCommitFailureRemovesStoredFile() throws Exception {
        var images = mock(MedicalImageRepository.class);
        var users = mock(UserRepository.class);
        var manager = manager();
        UUID ownerId = UUID.randomUUID();

        when(users.findById(ownerId)).thenReturn(Optional.of(
                new User("Test", "test@example.test", "test-hash")
        ));
        when(images.saveAndFlush(any(MedicalImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new TransactionSystemException("Simulated commit failure"))
                .when(manager).commit(any());

        var service = new ImageFileService(
                new ImageUploadValidator(),
                new StorageService(directory.toString()),
                images, users, manager
        );

        assertThrows(TransactionSystemException.class,
                () -> service.upload(ownerId, png()));
        assertDirectoryEmpty();
    }

    private PlatformTransactionManager manager() {
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
        return manager;
    }

    private void assertDirectoryEmpty() throws Exception {
        try (var files = Files.list(directory)) {
            assertEquals(0, files.count(), "Orphan file remains");
        }
    }

    private MockMultipartFile png() throws Exception {
        var image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        try (var output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, "png", output));
            return new MockMultipartFile(
                    "file", "scan.png", "image/png", output.toByteArray()
            );
        } finally {
            image.flush();
        }
    }
}

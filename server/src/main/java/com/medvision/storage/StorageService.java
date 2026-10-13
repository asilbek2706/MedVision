package com.medvision.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.*;

@Service
public class StorageService {

    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private final Path root;

    public StorageService(
            @Value("${medvision.storage.root:storage}") String location
    ) {
        try {
            Path configured = Path.of(location).toAbsolutePath().normalize();
            Files.createDirectories(configured);

            if (Files.isSymbolicLink(configured)) {
                throw new IOException("Storage root must not be a symbolic link.");
            }

            root = configured.toRealPath();
        } catch (IOException ex) {
            throw new IllegalStateException("Storage initialization failed.", ex);
        }
    }

    public String store(byte[] bytes, String extension) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES
                || !("png".equals(extension) || "jpg".equals(extension))) {
            throw new IllegalArgumentException("Invalid storage input.");
        }

        String key = UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        boolean created = false;

        try {
            try (var output = Files.newOutputStream(target, CREATE_NEW, WRITE)) {
                created = true;
                output.write(bytes);
            }
            return key;
        } catch (IOException ex) {
            if (created) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException cleanup) {
                    ex.addSuppressed(cleanup);
                }
            }
            throw new IllegalStateException("Image storage failed.", ex);
        }
    }

    public byte[] read(String key) {
        Path path = resolve(key);

        try {
            BasicFileAttributes attrs = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS
            );

            if (!attrs.isRegularFile()
                    || attrs.size() <= 0 || attrs.size() > MAX_BYTES) {
                throw new IOException("Invalid stored image.");
            }

            try (var input = Files.newInputStream(
                    path, READ, LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                    throw new IOException("Stored image size is invalid.");
                }
                return bytes;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Stored image could not be read.", ex);
        }
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            throw new IllegalStateException("Stored image could not be deleted.", ex);
        }
    }

    private Path resolve(String key) {
        if (key == null || !key.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)"
        )) {
            throw new IllegalArgumentException("Invalid storage key.");
        }

        Path path = root.resolve(key).normalize();
        if (!root.equals(path.getParent())) {
            throw new IllegalArgumentException("Storage path escaped root.");
        }
        return path;
    }
}

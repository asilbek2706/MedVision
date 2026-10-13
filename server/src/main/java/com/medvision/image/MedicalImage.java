package com.medvision.image;

import com.medvision.auth.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "medical_images")
public class MedicalImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MedicalImage() {
    }

    public MedicalImage(
            User user,
            String originalName,
            String storageKey,
            String mimeType,
            long sizeBytes,
            int width,
            int height
    ) {
        this.user = Objects.requireNonNull(user);
        this.originalName = Objects.requireNonNull(originalName);
        this.storageKey = Objects.requireNonNull(storageKey);
        this.mimeType = Objects.requireNonNull(mimeType);
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
    }

    @PrePersist
    private void beforeInsert() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getOriginalName() { return originalName; }
    public String getStorageKey() { return storageKey; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Instant getCreatedAt() { return createdAt; }
}
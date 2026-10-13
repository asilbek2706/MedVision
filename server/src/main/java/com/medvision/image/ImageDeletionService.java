package com.medvision.image;

import com.medvision.common.error.ApiException;
import com.medvision.storage.StorageCleanupService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
public class ImageDeletionService {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final StorageCleanupService cleanup;

    public ImageDeletionService(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactionManager,
            StorageCleanupService cleanup
    ) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
        this.cleanup = cleanup;
    }

    public void delete(UUID ownerId, UUID imageId) {
        Job job = transactions.execute(status -> {
            // Lock the owned row before checking dependent analyses.
            var keys = jdbc.query("""
                    SELECT storage_key FROM medical_images
                    WHERE id = ? AND user_id = ?
                    FOR UPDATE
                    """,
                    (row, number) -> row.getString("storage_key"),
                    imageId, ownerId
            );

            if (keys.isEmpty()) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND",
                        "So'ralgan resurs topilmadi."
                );
            }

            Boolean hasAnalyses = jdbc.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM analyses WHERE image_id = ?)",
                    Boolean.class,
                    imageId
            );

            if (Boolean.TRUE.equals(hasAnalyses)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "IMAGE_HAS_ANALYSES",
                        "Tasvirga bog'langan tahlillar mavjud."
                );
            }

            UUID jobId = UUID.randomUUID();
            String key = keys.getFirst();

            jdbc.update("""
                    INSERT INTO storage_deletion_jobs (id, storage_key)
                    VALUES (?, ?)
                    """, jobId, key);

            int deleted = jdbc.update("""
                    DELETE FROM medical_images
                    WHERE id = ? AND user_id = ?
                    """, imageId, ownerId);

            if (deleted != 1) {
                throw new IllegalStateException("Image deletion failed.");
            }

            return new Job(jobId, key);
        });

        if (job == null) {
            throw new IllegalStateException("Deletion transaction returned no job.");
        }

        // Physical cleanup starts only after the database commit.
        cleanup.attempt(job.id(), job.key());
    }

    private record Job(UUID id, String key) {
    }
}

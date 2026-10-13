package com.medvision.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StorageCleanupService {

    private static final Logger log =
            LoggerFactory.getLogger(StorageCleanupService.class);

    private final StorageService storage;
    private final JdbcTemplate jdbc;

    public StorageCleanupService(StorageService storage, JdbcTemplate jdbc) {
        this.storage = storage;
        this.jdbc = jdbc;
    }

    public void attempt(UUID jobId, String key) {
        try {
            storage.delete(key);
            jdbc.update(
                    "DELETE FROM storage_deletion_jobs WHERE id = ?",
                    jobId
            );
        } catch (RuntimeException ex) {
            // Keep the durable job and avoid logging private paths.
            log.warn("Storage cleanup deferred for job {}", jobId);

            try {
                jdbc.update("""
                        UPDATE storage_deletion_jobs
                        SET attempts = attempts + 1,
                            next_retry_at = CURRENT_TIMESTAMP + INTERVAL '1 minute'
                        WHERE id = ?
                        """, jobId);
            } catch (RuntimeException databaseFailure) {
                log.warn("Cleanup retry state update failed for job {}", jobId);
            }
        }
    }

    public void processPending() {
        var jobs = jdbc.query("""
                SELECT id, storage_key
                FROM storage_deletion_jobs
                WHERE next_retry_at <= CURRENT_TIMESTAMP
                ORDER BY created_at, id
                LIMIT 100
                """,
                (row, number) -> new Job(
                        row.getObject("id", UUID.class),
                        row.getString("storage_key")
                )
        );

        for (Job job : jobs) {
            attempt(job.id(), job.key());
        }
    }

    private record Job(UUID id, String key) {
    }
}

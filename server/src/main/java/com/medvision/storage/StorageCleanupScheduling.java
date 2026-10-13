package com.medvision.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "medvision.storage.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class StorageCleanupScheduling {

    private final StorageCleanupService cleanup;

    public StorageCleanupScheduling(StorageCleanupService cleanup) {
        this.cleanup = cleanup;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    public void retryPendingFiles() {
        cleanup.processPending();
    }
}

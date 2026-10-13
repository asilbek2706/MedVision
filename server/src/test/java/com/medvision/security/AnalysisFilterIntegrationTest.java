package com.medvision.security;

import com.medvision.analysis.AnalysisRepository;
import com.medvision.analysis.AnalysisStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class AnalysisFilterIntegrationTest {

    @Autowired
    private AnalysisRepository analyses;

    @Test
    void executesAllFilterCombinations() {
        UUID owner = UUID.randomUUID();
        var page = PageRequest.of(0, 10);

        assertNotNull(analyses.findFilteredByOwner(
                owner, null, null, null, null, page));

        assertNotNull(analyses.findFilteredByOwner(
                owner, AnalysisStatus.COMPLETED,
                "OTSU", null, null, page));

        assertNotNull(analyses.findFilteredByOwner(
                owner, null, null,
                Instant.parse("2026-01-01T00:00:00Z"),
                null, page));

        assertNotNull(analyses.findFilteredByOwner(
                owner, null, null, null,
                Instant.parse("2026-12-31T23:59:59Z"),
                page));

        assertNotNull(analyses.findFilteredByOwner(
                owner, AnalysisStatus.FAILED, "OTSU",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z"),
                page));
    }
}

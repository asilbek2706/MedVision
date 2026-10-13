package com.medvision.security;

public record DashboardStatsResponse(
        long totalImages,
        long completedAnalyses,
        long failedAnalyses
) {
}

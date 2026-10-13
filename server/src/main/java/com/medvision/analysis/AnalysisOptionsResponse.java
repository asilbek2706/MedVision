package com.medvision.analysis;

import java.util.List;

public record AnalysisOptionsResponse(
        List<String> plannedAlgorithms,
        List<String> acceptedFormats,
        long maxFileSizeBytes,
        int maxWidth,
        int maxHeight,
        boolean segmentationAvailable
) {
}
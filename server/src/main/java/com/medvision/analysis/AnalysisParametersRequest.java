package com.medvision.analysis;

import jakarta.validation.constraints.*;

public record AnalysisParametersRequest(
        @NotBlank(message = "Algorithm is required.")
        @Pattern(regexp = "OTSU", message = "Algorithm must be OTSU.")
        String algorithm,

        @NotNull(message = "Blur kernel size is required.")
        @Min(value = 1, message = "Blur kernel size must be at least 1.")
        @Max(value = 15, message = "Blur kernel size must not exceed 15.")
        Integer blurKernelSize,

        @NotNull(message = "Invert is required.")
        Boolean invert
) {
}
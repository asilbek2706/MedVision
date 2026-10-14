package com.medvision.analysis;

import jakarta.validation.constraints.*;

public record AnalysisParametersRequest(
        @NotBlank(message = "Analysis name is required.")
        @Size(max = 120, message = "Analysis name must not exceed 120 characters.")
        String name,

        @NotBlank(message = "Algorithm is required.")
        @Pattern(
                regexp = "OTSU|MANUAL|ADAPTIVE",
                message = "Algorithm must be OTSU, MANUAL or ADAPTIVE."
        )
        String algorithm,

        @NotNull(message = "Blur kernel size is required.")
        @Min(value = 1, message = "Blur kernel size must be at least 1.")
        @Max(value = 15, message = "Blur kernel size must not exceed 15.")
        Integer blurKernelSize,

        @NotNull(message = "Invert is required.")
        Boolean invert,

        @Min(value = 0, message = "Threshold must be at least 0.")
        @Max(value = 255, message = "Threshold must not exceed 255.")
        Integer threshold,

        @Min(value = 3, message = "Block size must be at least 3.")
        @Max(value = 31, message = "Block size must not exceed 31.")
        Integer blockSize,

        @DecimalMin(value = "-50.0", message = "C must be at least -50.")
        @DecimalMax(value = "50.0", message = "C must not exceed 50.")
        Double c
) {

    public AnalysisParametersRequest(
            String name,
            String algorithm,
            Integer blurKernelSize,
            Boolean invert
    ) {
        this(
                name,
                algorithm,
                blurKernelSize,
                invert,
                null,
                null,
                null
        );
    }
}
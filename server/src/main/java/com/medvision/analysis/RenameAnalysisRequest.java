package com.medvision.analysis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameAnalysisRequest(
        @NotBlank(message = "Analysis name is required.")
        @Size(max = 120, message = "Analysis name must not exceed 120 characters.")
        String name
) {
}
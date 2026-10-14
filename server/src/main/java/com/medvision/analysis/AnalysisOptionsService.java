package com.medvision.analysis;

import com.medvision.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisOptionsService {

    public AnalysisOptionsResponse getOptions() {
        return new AnalysisOptionsResponse(
                List.of("OTSU", "MANUAL", "ADAPTIVE"),
                List.of("PNG", "JPEG"),
                10L * 1024 * 1024,
                4096,
                4096,
                true
        );
    }

    public AnalysisParametersRequest validateParameters(
            AnalysisParametersRequest request
    ) {
        if (request == null
                || request.algorithm() == null
                || request.blurKernelSize() == null) {
            invalidParameters();
        }

        int blur = request.blurKernelSize();

        if (blur < 1 || blur > 15 || blur % 2 == 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_BLUR_KERNEL",
                    "Blur kernel must be odd and between 1 and 15."
            );
        }

        switch (request.algorithm()) {
            case "OTSU" -> {
                if (request.threshold() != null
                        || request.blockSize() != null
                        || request.c() != null) {
                    invalidParameters();
                }
            }

            case "MANUAL" -> {
                Integer threshold = request.threshold();

                if (threshold == null
                        || threshold < 0
                        || threshold > 255
                        || request.blockSize() != null
                        || request.c() != null) {
                    invalidParameters();
                }
            }

            case "ADAPTIVE" -> {
                Integer blockSize = request.blockSize();
                Double c = request.c();

                if (request.threshold() != null
                        || blockSize == null
                        || blockSize < 3
                        || blockSize > 31
                        || blockSize % 2 == 0
                        || c == null
                        || !Double.isFinite(c)
                        || c < -50
                        || c > 50) {
                    invalidParameters();
                }
            }

            default -> invalidParameters();
        }

        return request;
    }

    private void invalidParameters() {
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_ALGORITHM_PARAMETERS",
                "Invalid parameters for selected algorithm."
        );
    }
}
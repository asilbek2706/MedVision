package com.medvision.analysis;

import com.medvision.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisOptionsService {

    public AnalysisOptionsResponse getOptions() {
        return new AnalysisOptionsResponse(
                List.of("OTSU"),
                List.of("PNG", "JPEG"),
                10L * 1024 * 1024,
                4096,
                4096,
                false
        );
    }

    public AnalysisParametersRequest validateParameters(
            AnalysisParametersRequest request
    ) {
        if (request.blurKernelSize() % 2 == 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_BLUR_KERNEL",
                    "Blur kernel o‘lchami toq son bo‘lishi kerak."
            );
        }

        return request;
    }
}
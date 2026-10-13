package com.medvision.analysis;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis-options")
public class AnalysisOptionsController {

    private final AnalysisOptionsService service;

    public AnalysisOptionsController(AnalysisOptionsService service) {
        this.service = service;
    }

    @GetMapping
    public AnalysisOptionsResponse getOptions() {
        return service.getOptions();
    }

    @PostMapping("/validate")
    public AnalysisParametersRequest validateParameters(
            @Valid @RequestBody AnalysisParametersRequest request
    ) {
        return service.validateParameters(request);
    }
}
package com.medvision.analysis;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisEvaluationController {

    private final AnalysisEvaluationExecutionService service;

    public AnalysisEvaluationController(
            AnalysisEvaluationExecutionService service
    ) {
        this.service = service;
    }

    @PostMapping(
            value = "/{id}/evaluate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public AnalysisResponse evaluate(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        UUID ownerId = UUID.fromString(
                authentication.getName()
        );

        return service.evaluate(
                ownerId,
                id,
                file
        );
    }
}
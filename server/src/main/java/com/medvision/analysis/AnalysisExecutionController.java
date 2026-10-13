package com.medvision.analysis;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/images/{imageId}/analyses")
public class AnalysisExecutionController {

    private final AnalysisExecutionService service;

    public AnalysisExecutionController(
            AnalysisExecutionService service
    ) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> execute(
            Authentication authentication,
            @PathVariable UUID imageId,
            @Valid @RequestBody AnalysisParametersRequest request
    ) {
        UUID ownerId = UUID.fromString(
                authentication.getName()
        );

        AnalysisResponse response = service.execute(
                ownerId,
                imageId,
                request
        );

        return ResponseEntity.created(
                URI.create("/api/analyses/" + response.id())
        ).body(response);
    }
}
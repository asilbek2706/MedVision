package com.medvision.security;

import com.medvision.analysis.AnalysisResponse;
import com.medvision.image.MedicalImageResponse;
import com.medvision.common.pagination.PageResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class OwnedResourceController {

    private final OwnedResourceService resources;

    public OwnedResourceController(OwnedResourceService resources) {
        this.resources = resources;
    }

    @GetMapping("/images")
    public PageResponse<MedicalImageResponse> images(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return resources.images(ownerId(authentication), page, size);
    }

    @GetMapping("/images/{id}")
    public MedicalImageResponse image(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return resources.image(ownerId(authentication), id);
    }

    @GetMapping("/analyses")
    public PageResponse<AnalysisResponse> analyses(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return resources.analyses(ownerId(authentication), page, size);
    }

    @GetMapping("/analyses/{id}")
    public AnalysisResponse analysis(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return resources.analysis(ownerId(authentication), id);
    }

    @GetMapping("/images/{id}/analyses")
    public PageResponse<AnalysisResponse> imageAnalyses(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return resources.imageAnalyses(ownerId(authentication), id, page, size);
    }

    private static UUID ownerId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}

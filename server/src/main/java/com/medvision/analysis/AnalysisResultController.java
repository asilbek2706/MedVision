package com.medvision.analysis;

import com.medvision.common.error.ApiException;
import com.medvision.storage.StorageService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisResultController {

    private final AnalysisRepository analyses;
    private final StorageService storage;

    public AnalysisResultController(
            AnalysisRepository analyses,
            StorageService storage
    ) {
        this.analyses = analyses;
        this.storage = storage;
    }

    @GetMapping("/{id}/mask")
    public ResponseEntity<byte[]> mask(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return getResult(authentication, id, true, false);
    }

    @GetMapping("/{id}/overlay")
    public ResponseEntity<byte[]> overlay(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return getResult(authentication, id, false, false);
    }

    @GetMapping("/{id}/mask/download")
    public ResponseEntity<byte[]> downloadMask(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return getResult(authentication, id, true, true);
    }

    @GetMapping("/{id}/overlay/download")
    public ResponseEntity<byte[]> downloadOverlay(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return getResult(authentication, id, false, true);
    }

    private ResponseEntity<byte[]> getResult(
            Authentication authentication,
            UUID id,
            boolean mask,
            boolean download
    ) {
        UUID ownerId = UUID.fromString(authentication.getName());

        Analysis analysis = analyses
                .findByIdAndImage_User_Id(id, ownerId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND",
                        "Analysis not found."
                ));

        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ANALYSIS_NOT_COMPLETED",
                    "Analysis is not completed."
            );
        }

        String key = mask
                ? analysis.getMaskKey()
                : analysis.getOverlayKey();

        byte[] bytes = storage.read(key);

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(bytes.length)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff");

        if (download) {
            String filename = id + (mask ? "-mask.png" : "-overlay.png");

            response.header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                            .filename(filename)
                            .build()
                            .toString()
            );
        }

        return response.body(bytes);
    }
}
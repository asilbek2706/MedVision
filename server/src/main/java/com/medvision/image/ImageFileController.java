package com.medvision.image;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageFileController {

    private final ImageFileService files;

    public ImageFileController(ImageFileService files) {
        this.files = files;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicalImageResponse> upload(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        var image = files.upload(
                UUID.fromString(authentication.getName()), file
        );

        return ResponseEntity.created(
                URI.create("/api/images/" + image.id())
        ).body(image);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        var image = files.content(
                UUID.fromString(authentication.getName()), id
        );

        String extension = "image/png".equals(image.mimeType()) ? "png" : "jpg";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mimeType()))
                .contentLength(image.bytes().length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(id + "." + extension)
                                .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.bytes());
    }
}

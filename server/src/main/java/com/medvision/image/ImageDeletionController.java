package com.medvision.image;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageDeletionController {

    private final ImageDeletionService deletion;

    public ImageDeletionController(ImageDeletionService deletion) {
        this.deletion = deletion;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        deletion.delete(
                UUID.fromString(authentication.getName()), id
        );
        return ResponseEntity.noContent().build();
    }
}

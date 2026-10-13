package com.medvision.database;

import com.medvision.auth.*;
import com.medvision.image.*;
import com.medvision.analysis.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResponseSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository users;

    @Autowired
    private MedicalImageRepository images;

    @Autowired
    private AnalysisRepository analyses;

    @Test
    void responseDtosExcludeInternalFields() throws Exception {
        String privateHash = "private-password-hash-marker";
        String privateOriginalKey = "private-original-storage-marker";
        String privateMaskKey = "private-mask-storage-marker";
        String privateOverlayKey = "private-overlay-storage-marker";

        User user = users.saveAndFlush(new User(
                "DTO Test",
                UUID.randomUUID() + "@example.test",
                privateHash
        ));

        MedicalImage image = images.saveAndFlush(new MedicalImage(
                user,
                "sample.png",
                privateOriginalKey + UUID.randomUUID(),
                "image/png",
                1024,
                100,
                100
        ));

        Analysis analysis = analyses.saveAndFlush(new Analysis(
                image, "OTSU", Map.of("blurKernelSize", 5)
        ));

        analysis.complete(privateMaskKey, privateOverlayKey, 2500, 0.25, 120);
        analyses.flush();

        var userJson = objectMapper.readTree(
                objectMapper.writeValueAsString(UserResponse.from(user))
        );
        assertEquals(user.getId().toString(), userJson.get("id").asText());
        assertFalse(userJson.has("passwordHash"));
        assertFalse(userJson.toString().contains(privateHash));

        var imageJson = objectMapper.readTree(
                objectMapper.writeValueAsString(MedicalImageResponse.from(image))
        );
        assertEquals("sample.png", imageJson.get("originalName").asText());
        assertFalse(imageJson.has("storageKey"));
        assertFalse(imageJson.has("user"));
        assertFalse(imageJson.toString().contains(privateOriginalKey));

        var analysisJson = objectMapper.readTree(
                objectMapper.writeValueAsString(AnalysisResponse.from(analysis))
        );
        assertEquals(image.getId().toString(), analysisJson.get("imageId").asText());
        assertEquals("COMPLETED", analysisJson.get("status").asText());
        assertFalse(analysisJson.has("maskKey"));
        assertFalse(analysisJson.has("overlayKey"));
        assertFalse(analysisJson.has("image"));
        assertFalse(analysisJson.toString().contains(privateMaskKey));
        assertFalse(analysisJson.toString().contains(privateOverlayKey));

        assertTrue(userJson.get("createdAt").asText().endsWith("Z"));
        assertTrue(analysisJson.get("completedAt").asText().endsWith("Z"));
    }
}
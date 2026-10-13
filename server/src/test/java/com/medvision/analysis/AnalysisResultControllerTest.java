package com.medvision.analysis;

import com.medvision.common.error.ApiException;
import com.medvision.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisResultControllerTest {

    private AnalysisRepository analyses;
    private StorageService storage;
    private AnalysisResultController controller;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID analysisId = UUID.randomUUID();

    private final byte[] png = new byte[]{
            (byte) 137, 80, 78, 71, 13, 10, 26, 10
    };

    @BeforeEach
    void setup() {
        analyses = mock(AnalysisRepository.class);
        storage = mock(StorageService.class);
        controller = new AnalysisResultController(analyses, storage);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                ownerId.toString(), null
        );
    }

    private Analysis completedAnalysis() {
        var image = mock(com.medvision.image.MedicalImage.class);
        when(image.getWidth()).thenReturn(100);
        when(image.getHeight()).thenReturn(100);

        Analysis analysis = new Analysis(
                image,
                "OTSU",
                Map.of()
        );

        analysis.complete("mask.png", "overlay.png", 2500, 0.25, 100);
        return analysis;
    }

    @Test
    void maskDownloadHasAttachmentHeadersAndCorrectBytes() {
        Analysis analysis = completedAnalysis();

        when(analyses.findByIdAndImage_User_Id(analysisId, ownerId))
                .thenReturn(Optional.of(analysis));

        when(storage.read("mask.png")).thenReturn(png);

        var response = controller.downloadMask(authentication(), analysisId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
        assertArrayEquals(png, response.getBody());

        String disposition = response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION);

        assertNotNull(disposition);
        assertTrue(disposition.contains("attachment"));
        assertTrue(disposition.contains(analysisId + "-mask.png"));

        assertEquals("no-store", response.getHeaders()
                .getCacheControl());
        assertEquals("nosniff", response.getHeaders()
                .getFirst("X-Content-Type-Options"));
    }

    @Test
    void overlayDownloadReturnsCorrectFile() {
        Analysis analysis = completedAnalysis();

        when(analyses.findByIdAndImage_User_Id(analysisId, ownerId))
                .thenReturn(Optional.of(analysis));

        when(storage.read("overlay.png")).thenReturn(png);

        var response = controller.downloadOverlay(authentication(), analysisId);

        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(png, response.getBody());

        String disposition = response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION);

        assertNotNull(disposition);
        assertTrue(disposition.contains(analysisId + "-overlay.png"));

        verify(storage).read("overlay.png");
        verify(storage, never()).read("mask.png");
    }

    @Test
    void anotherUserCannotDownloadAnalysis() {
        when(analyses.findByIdAndImage_User_Id(analysisId, ownerId))
                .thenReturn(Optional.empty());

        ApiException error = assertThrows(
                ApiException.class,
                () -> controller.downloadMask(authentication(), analysisId)
        );

        assertEquals(404, error.getStatus().value());
        verifyNoInteractions(storage);
    }

    @Test
    void processingAnalysisCannotBeDownloaded() {
        Analysis processing = new Analysis(
                mock(com.medvision.image.MedicalImage.class),
                "OTSU",
                Map.of()
        );

        when(analyses.findByIdAndImage_User_Id(analysisId, ownerId))
                .thenReturn(Optional.of(processing));

        ApiException error = assertThrows(
                ApiException.class,
                () -> controller.downloadOverlay(authentication(), analysisId)
        );

        assertEquals(409, error.getStatus().value());
        assertEquals("ANALYSIS_NOT_COMPLETED", error.getCode());
        verifyNoInteractions(storage);
    }

    @Test
    void previewEndpointsDoNotForceDownload() {
        Analysis analysis = completedAnalysis();

        when(analyses.findByIdAndImage_User_Id(analysisId, ownerId))
                .thenReturn(Optional.of(analysis));

        when(storage.read("mask.png")).thenReturn(png);

        var response = controller.mask(authentication(), analysisId);

        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals(png, response.getBody());
    }
}
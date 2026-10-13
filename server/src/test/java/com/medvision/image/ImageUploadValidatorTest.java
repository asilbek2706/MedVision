package com.medvision.image;

import com.medvision.common.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ImageUploadValidatorTest {

    private final ImageUploadValidator validator = new ImageUploadValidator();

    @Test
    void acceptsRealPngAndJpeg() throws Exception {
        for (String format : new String[]{"png", "jpeg"}) {
            String mime = "png".equals(format) ? "image/png" : "image/jpeg";
            var result = validator.validate(new MockMultipartFile(
                    "file", "scan." + format, mime, image(format, 12, 9)
            ));

            assertEquals(12, result.width());
            assertEquals(9, result.height());
            assertEquals(mime, result.mimeType());
        }
    }

    @Test
    void removesClientPathFromMetadataName() throws Exception {
        var result = validator.validate(new MockMultipartFile(
                "file", "..\\..\\scan.png", "image/png", image("png", 2, 2)
        ));
        assertEquals("scan.png", result.originalName());
    }

    @Test
    void rejectsMimeAndExtensionSpoofing() throws Exception {
        byte[] png = image("png", 2, 2);

        assertCode("IMAGE_TYPE_MISMATCH", new MockMultipartFile(
                "file", "scan.jpg", "image/jpeg", png
        ));
        assertCode("IMAGE_TYPE_MISMATCH", new MockMultipartFile(
                "file", "scan.png", "image/jpeg", png
        ));
    }

    @Test
    void rejectsTextRenamedAsImage() {
        assertCode("UNSUPPORTED_IMAGE", new MockMultipartFile(
                "file", "scan.png", "image/png",
                "this is not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        ));
    }

    @Test
    void rejectsTruncatedImage() throws Exception {
        byte[] png = image("png", 2, 2);
        assertCode("INVALID_IMAGE", new MockMultipartFile(
                "file", "scan.png", "image/png",
                Arrays.copyOf(png, png.length - 5)
        ));
    }

    @Test
    void rejectsEmptyAndOversizedFiles() {
        assertCode("EMPTY_IMAGE", new MockMultipartFile(
                "file", "scan.png", "image/png", new byte[0]
        ));

        ApiException error = assertThrows(ApiException.class,
                () -> validator.validate(new MockMultipartFile(
                        "file", "scan.png", "image/png",
                        new byte[ImageUploadValidator.MAX_BYTES + 1]
                )));
        assertEquals(413, error.getStatus().value());
    }

    @Test
    void rejectsExcessiveWidthBeforeFullDecoding() throws Exception {
        assertCode("IMAGE_DIMENSIONS_EXCEEDED", new MockMultipartFile(
                "file", "scan.png", "image/png", image("png", 4097, 1)
        ));
    }

    private void assertCode(String code, MockMultipartFile file) {
        ApiException error = assertThrows(
                ApiException.class, () -> validator.validate(file)
        );
        assertEquals(code, error.getCode());
    }

    private byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB
        );
        try (var output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, format, output));
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }
}
package com.medvision.image;

import com.medvision.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ImageUploadValidator {

    public static final int MAX_BYTES = 10 * 1024 * 1024;
    public static final int MAX_DIMENSION = 4096;
    public static final long MAX_PIXELS = 16_000_000L;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 137, 80, 78, 71, 13, 10, 26, 10
    };

    private static final byte[] PNG_END = {
            0, 0, 0, 0, 73, 69, 78, 68,
            (byte) 174, 66, 96, (byte) 130
    };

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST,
                    "EMPTY_IMAGE", "Rasm fayli bo'sh.");
        }

        if (file.getSize() > MAX_BYTES) {
            throw tooLarge();
        }

        byte[] bytes;
        try (var input = file.getInputStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException ex) {
            throw invalid();
        }

        if (bytes.length == 0) {
            throw error(HttpStatus.BAD_REQUEST,
                    "EMPTY_IMAGE", "Rasm fayli bo'sh.");
        }
        if (bytes.length > MAX_BYTES) {
            throw tooLarge();
        }

        String name = safeName(file.getOriginalFilename());
        String extension = extension(name);
        String declaredMime = file.getContentType();

        ImageReader reader = null;

        try (var stream = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(bytes))) {

            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw unsupported();
            }

            reader = readers.next();
            String format = reader.getFormatName().toLowerCase(Locale.ROOT);

            String mime;
            String storageExtension;

            if ("png".equals(format)) {
                mime = "image/png";
                storageExtension = "png";
                if (!"png".equals(extension)) {
                    throw mismatch();
                }
                if (bytes.length < 20
                        || !Arrays.equals(
                                Arrays.copyOfRange(bytes, 0, 8), PNG_SIGNATURE)
                        || !Arrays.equals(
                                Arrays.copyOfRange(bytes,
                                        bytes.length - 12, bytes.length),
                                PNG_END)) {
                    throw invalid();
                }
            } else if ("jpeg".equals(format) || "jpg".equals(format)) {
                mime = "image/jpeg";
                storageExtension = "jpg";
                if (!"jpg".equals(extension) && !"jpeg".equals(extension)) {
                    throw mismatch();
                }
                if (bytes.length < 4
                        || (bytes[0] & 255) != 255
                        || (bytes[1] & 255) != 216
                        || (bytes[bytes.length - 2] & 255) != 255
                        || (bytes[bytes.length - 1] & 255) != 217) {
                    throw invalid();
                }
            } else {
                throw unsupported();
            }

            if (declaredMime != null && !declaredMime.isBlank()
                    && !"application/octet-stream".equalsIgnoreCase(declaredMime)
                    && !mime.equalsIgnoreCase(declaredMime)) {
                throw mismatch();
            }

            AtomicBoolean warned = new AtomicBoolean();
            reader.addIIOReadWarningListener(
                    (source, warning) -> warned.set(true)
            );
            reader.setInput(stream, true, true);

            int width = reader.getWidth(0);
            int height = reader.getHeight(0);

            if (width < 1 || height < 1
                    || width > MAX_DIMENSION || height > MAX_DIMENSION
                    || (long) width * height > MAX_PIXELS) {
                throw error(HttpStatus.BAD_REQUEST,
                        "IMAGE_DIMENSIONS_EXCEEDED",
                        "Tasvir 4096x4096 va 16 million piksel limitidan oshmasligi kerak.");
            }

            // Full decoding happens only after the dimensions are bounded.
            BufferedImage decoded = reader.read(0);
            if (decoded == null || warned.get()) {
                if (decoded != null) decoded.flush();
                throw invalid();
            }

            try {
                if (decoded.getWidth() != width || decoded.getHeight() != height) {
                    throw invalid();
                }
            } finally {
                decoded.flush();
            }

            return new ValidatedImage(
                    bytes, name, mime, storageExtension, width, height
            );
        } catch (IOException | IllegalArgumentException ex) {
            throw invalid();
        } finally {
            if (reader != null) reader.dispose();
        }
    }

    private static String safeName(String original) {
        if (original == null || original.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST,
                    "INVALID_FILENAME", "Fayl nomi kerak.");
        }

        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();

        if (name.isBlank() || name.length() > 255
                || name.chars().anyMatch(Character::isISOControl)) {
            throw error(HttpStatus.BAD_REQUEST,
                    "INVALID_FILENAME", "Fayl nomi noto'g'ri.");
        }

        return name;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static ApiException tooLarge() {
        return error(HttpStatus.PAYLOAD_TOO_LARGE,
                "IMAGE_TOO_LARGE", "Rasm hajmi 10 MiB dan oshmasligi kerak.");
    }

    private static ApiException unsupported() {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_IMAGE", "Faqat PNG va JPEG rasmlar qabul qilinadi.");
    }

    private static ApiException mismatch() {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "IMAGE_TYPE_MISMATCH",
                "Fayl kengaytmasi, MIME va haqiqiy tasvir formati mos emas.");
    }

    private static ApiException invalid() {
        return error(HttpStatus.BAD_REQUEST,
                "INVALID_IMAGE", "Tasvir buzilgan yoki to'liq dekodlanmadi.");
    }

    private static ApiException error(
            HttpStatus status, String code, String message
    ) {
        return new ApiException(status, code, message);
    }

    public record ValidatedImage(
            byte[] bytes,
            String originalName,
            String mimeType,
            String extension,
            int width,
            int height
    ) {
    }
}
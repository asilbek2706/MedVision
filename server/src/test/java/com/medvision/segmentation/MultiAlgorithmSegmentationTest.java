package com.medvision.segmentation;

import com.medvision.segmentation.algorithms.*;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.Test;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiAlgorithmSegmentationTest {

    static {
        OpenCV.loadLocally();
    }

    private final SegmentationService service =
            new SegmentationService(
                    new AlgorithmRegistry(List.of(
                            new OtsuThresholdAlgorithm(),
                            new ManualThresholdAlgorithm(),
                            new AdaptiveThresholdAlgorithm()
                    ))
            );

    @Test
    void allAlgorithmsProcessSameImage() {
        byte[] image = createTestImage();

        SegmentationService.Options options =
                new SegmentationService.Options(
                        false, 1, 1, 0
                );

        String[] algorithms = {
                "OTSU", "MANUAL", "ADAPTIVE"
        };

        AlgorithmParameters[] parameters = {
                AlgorithmParameters.otsu(false),
                AlgorithmParameters.manual(false, 127),
                AlgorithmParameters.adaptive(false, 3, 5.0)
        };

        for (int i = 0; i < algorithms.length; i++) {
            SegmentationService.Result result =
                    service.segment(
                            image,
                            options,
                            algorithms[i],
                            parameters[i]
                    );

            assertEquals(64, result.width());
            assertEquals(64, result.height());
            assertTrue(result.maskPng().length > 0);
            assertTrue(result.overlayPng().length > 0);

            MatOfByte encoded = new MatOfByte(
                    result.maskPng()
            );

            Mat mask = new Mat();

            try {
                mask = Imgcodecs.imdecode(
                        encoded,
                        Imgcodecs.IMREAD_GRAYSCALE
                );

                assertFalse(mask.empty());
                assertEquals(64, mask.rows());
                assertEquals(64, mask.cols());

                assertTrue(
                        result.foregroundPixels() >= 0
                );

                assertTrue(
                        result.foregroundPixels() <= 4096
                );
            } finally {
                mask.release();
                encoded.release();
            }
        }
    }

    @Test
    void rejectsUnsupportedAlgorithm() {
        byte[] image = createTestImage();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.segment(
                        image,
                        SegmentationService.Options.defaults(),
                        "UNKNOWN",
                        AlgorithmParameters.otsu(false)
                )
        );
    }

    @Test
    void rejectsMismatchedInvertSettings() {
        byte[] image = createTestImage();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.segment(
                        image,
                        SegmentationService.Options.defaults(),
                        "MANUAL",
                        AlgorithmParameters.manual(true, 127)
                )
        );
    }

    private static byte[] createTestImage() {
        Mat image = new Mat(
                64,
                64,
                CvType.CV_8UC1,
                new Scalar(40)
        );

        MatOfByte buffer = new MatOfByte();

        try {
            image.submat(16, 48, 16, 48)
                    .setTo(new Scalar(220));

            assertTrue(
                    Imgcodecs.imencode(".png", image, buffer)
            );

            return buffer.toArray();
        } finally {
            buffer.release();
            image.release();
        }
    }
}
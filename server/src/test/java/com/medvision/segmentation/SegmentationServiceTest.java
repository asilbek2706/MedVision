package com.medvision.segmentation;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.Test;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import static org.junit.jupiter.api.Assertions.*;

class SegmentationServiceTest {

    static {
        OpenCV.loadLocally();
    }

    private final SegmentationService service =
            new SegmentationService();

    private byte[] makeImage(
            int width, int height,
            boolean addMainObject,
            boolean addNoise
    ) {
        Mat image = new Mat(
                height, width, CvType.CV_8UC3,
                new Scalar(0, 0, 0)
        );

        MatOfByte encoded = new MatOfByte();

        try {
            if (addMainObject) {
                Imgproc.rectangle(
                        image,
                        new Point(20, 20),
                        new Point(79, 79),
                        new Scalar(255, 255, 255),
                        -1
                );
            }

            if (addNoise) {
                Imgproc.rectangle(
                        image,
                        new Point(5, 5),
                        new Point(7, 7),
                        new Scalar(255, 255, 255),
                        -1
                );
            }

            assertTrue(
                    Imgcodecs.imencode(".png", image, encoded)
            );

            return encoded.toArray();
        } finally {
            encoded.release();
            image.release();
        }
    }

    private Mat decode(byte[] png) {
        MatOfByte bytes = new MatOfByte(png);

        try {
            return Imgcodecs.imdecode(
                    bytes, Imgcodecs.IMREAD_UNCHANGED
            );
        } finally {
            bytes.release();
        }
    }

    @Test
    void createsBinaryMaskAndOverlay() {
        byte[] input = makeImage(100, 100, true, false);

        var result = service.segment(
                input,
                new SegmentationService.Options(
                        false, 3, 3, 50
                )
        );

        assertEquals(100, result.width());
        assertEquals(100, result.height());

        assertTrue(result.foregroundPixels() > 3000);
        assertTrue(result.foregroundPixels() < 4200);

        assertEquals(
                result.foregroundPixels() / 100.0,
                result.foregroundPercent(),
                0.001
        );

        assertFalse(result.emptyMask());
        assertFalse(result.fullMask());

        Mat mask = decode(result.maskPng());
        Mat overlay = decode(result.overlayPng());

        try {
            assertFalse(mask.empty());
            assertFalse(overlay.empty());

            assertEquals(1, mask.channels());
            assertEquals(3, overlay.channels());

            assertEquals(100, mask.rows());
            assertEquals(100, overlay.cols());

            assertEquals(0.0, mask.get(0, 0)[0]);
            assertEquals(255.0, mask.get(50, 50)[0]);

            assertEquals(
                    result.foregroundPixels(),
                    Core.countNonZero(mask)
            );

            double[] overlayPixel = overlay.get(50, 50);
            assertTrue(overlayPixel[2] > overlayPixel[0]);

        } finally {
            mask.release();
            overlay.release();
        }
    }

    @Test
    void supportsInvertedForeground() {
        byte[] input = makeImage(100, 100, true, false);

        var normal = service.segment(
                input,
                new SegmentationService.Options(false, 3, 1, 0)
        );

        var inverted = service.segment(
                input,
                new SegmentationService.Options(true, 3, 1, 0)
        );

        assertTrue(normal.foregroundPixels() > 0);
        assertTrue(inverted.foregroundPixels() > 0);

        assertEquals(
                10000L,
                normal.foregroundPixels()
                        + inverted.foregroundPixels()
        );
    }

    @Test
    void removesSmallComponents() {
        byte[] input = makeImage(100, 100, true, true);

        var result = service.segment(
                input,
                new SegmentationService.Options(false, 1, 1, 50)
        );

        Mat mask = decode(result.maskPng());

        try {
            assertEquals(0.0, mask.get(6, 6)[0]);
            assertEquals(255.0, mask.get(50, 50)[0]);
        } finally {
            mask.release();
        }
    }

    @Test
    void detectsEmptyMask() {
        byte[] input = makeImage(100, 100, false, false);

        var result = service.segment(
                input,
                new SegmentationService.Options(false, 3, 3, 50)
        );

        assertEquals(0, result.foregroundPixels());
        assertEquals(0.0, result.foregroundPercent());
        assertTrue(result.emptyMask());
        assertFalse(result.fullMask());
    }

    @Test
    void rejectsInvalidOptionsAndInput() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SegmentationService.Options(
                        false, 4, 3, 50
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SegmentationService.Options(
                        false, 3, 2, 50
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.segment(
                        new byte[0],
                        SegmentationService.Options.defaults()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.segment(
                        new byte[]{1, 2, 3, 4},
                        SegmentationService.Options.defaults()
                )
        );
    }
}
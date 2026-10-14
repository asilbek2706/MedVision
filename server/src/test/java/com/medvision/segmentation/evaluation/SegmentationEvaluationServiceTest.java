package com.medvision.segmentation.evaluation;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import static org.junit.jupiter.api.Assertions.*;

class SegmentationEvaluationServiceTest {

    private final SegmentationEvaluationService service =
            new SegmentationEvaluationService();

    @BeforeAll
    static void loadOpenCv() {
        OpenCV.loadLocally();
    }

    @Test
    void evaluatesIdenticalPngMasks() {
        byte[] mask = png(255, 0, 255, 0);

        var result = service.evaluate(mask, mask);

        assertEquals(1.0, result.dice(), 1e-9);
        assertEquals(1.0, result.iou(), 1e-9);
        assertEquals(2, result.truePositive());
    }

    @Test
    void evaluatesPartialOverlap() {
        byte[] prediction = png(255, 255, 0, 0);
        byte[] truth = png(255, 0, 255, 0);

        var result = service.evaluate(prediction, truth);

        assertEquals(0.5, result.dice(), 1e-9);
        assertEquals(1.0 / 3.0, result.iou(), 1e-9);
        assertEquals(1, result.truePositive());
        assertEquals(1, result.falsePositive());
        assertEquals(1, result.falseNegative());
    }

    @Test
    void rejectsMissingGroundTruth() {
        byte[] prediction = png(255, 0, 0, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.evaluate(prediction, null)
        );
    }

    @Test
    void rejectsInvalidPng() {
        byte[] invalid = new byte[] {1, 2, 3, 4};
        byte[] truth = png(255, 0, 0, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.evaluate(invalid, truth)
        );
    }

    @Test
    void rejectsDifferentDimensions() {
        byte[] prediction = png(255, 0, 0, 0);
        byte[] truth = png(255, 0, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.evaluate(prediction, truth)
        );
    }

    private static byte[] png(int... pixels) {
        Mat mask = new Mat(1, pixels.length, CvType.CV_8UC1);
        MatOfByte encoded = new MatOfByte();

        try {
            byte[] values = new byte[pixels.length];

            for (int i = 0; i < pixels.length; i++) {
                values[i] = (byte) pixels[i];
            }

            mask.put(0, 0, values);

            if (!Imgcodecs.imencode(".png", mask, encoded)) {
                throw new IllegalStateException("PNG encoding failed.");
            }

            return encoded.toArray();
        } finally {
            encoded.release();
            mask.release();
        }
    }
}
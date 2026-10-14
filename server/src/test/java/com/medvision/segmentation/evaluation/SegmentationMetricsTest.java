package com.medvision.segmentation.evaluation;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import static org.junit.jupiter.api.Assertions.*;

class SegmentationMetricsTest {

    @BeforeAll
    static void loadOpenCv() {
        OpenCV.loadLocally();
    }

    @Test
    void perfectMatch() {
        Mat prediction = mask(255, 0, 255, 0);
        Mat truth = mask(255, 0, 255, 0);
        try {

            var result = SegmentationMetrics.calculate(prediction, truth);

            assertEquals(1.0, result.dice(), 1e-9);
            assertEquals(1.0, result.iou(), 1e-9);
            assertEquals(2, result.truePositive());
            assertEquals(0, result.falsePositive());
            assertEquals(0, result.falseNegative());
        } finally {
            prediction.release();
            truth.release();
        }
    }

    @Test
    void partialOverlap() {
        Mat prediction = mask(255, 255, 0, 0);
        Mat truth = mask(255, 0, 255, 0);
        try {

            var result = SegmentationMetrics.calculate(prediction, truth);

            assertEquals(0.5, result.dice(), 1e-9);
            assertEquals(1.0 / 3.0, result.iou(), 1e-9);
            assertEquals(1, result.truePositive());
            assertEquals(1, result.falsePositive());
            assertEquals(1, result.falseNegative());
        } finally {
            prediction.release();
            truth.release();
        }
    }

    @Test
    void noOverlap() {
        Mat prediction = mask(255, 0, 0, 0);
        Mat truth = mask(0, 255, 0, 0);
        try {

            var result = SegmentationMetrics.calculate(prediction, truth);

            assertEquals(0.0, result.dice(), 1e-9);
            assertEquals(0.0, result.iou(), 1e-9);
        } finally {
            prediction.release();
            truth.release();
        }
    }

    @Test
    void bothMasksHaveNoForeground() {
        Mat prediction = mask(0, 0, 0, 0);
        Mat truth = mask(0, 0, 0, 0);
        try {

            var result = SegmentationMetrics.calculate(prediction, truth);

            assertEquals(1.0, result.dice(), 1e-9);
            assertEquals(1.0, result.iou(), 1e-9);
        } finally {
            prediction.release();
            truth.release();
        }
    }

    @Test
    void rejectsDifferentDimensions() {
        Mat prediction = mask(255, 0, 0, 0);
        Mat truth = new Mat(2, 2, CvType.CV_8UC1);
        try {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> SegmentationMetrics.calculate(prediction, truth)
            );
        } finally {
            prediction.release();
            truth.release();
        }
    }

    @Test
    void rejectsWrongMaskType() {
        Mat prediction = mask(255, 0, 0, 0);
        Mat truth = new Mat(1, 4, CvType.CV_8UC3);
        try {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> SegmentationMetrics.calculate(prediction, truth)
            );
        } finally {
            prediction.release();
            truth.release();
        }
    }

    private static Mat mask(int... pixels) {
        Mat mat = new Mat(1, pixels.length, CvType.CV_8UC1);

        byte[] values = new byte[pixels.length];

        for (int i = 0; i < pixels.length; i++) {
            values[i] = (byte) pixels[i];
        }

        mat.put(0, 0, values);
        return mat;
    }
}
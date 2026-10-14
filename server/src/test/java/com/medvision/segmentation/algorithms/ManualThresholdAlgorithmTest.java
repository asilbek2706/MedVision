package com.medvision.segmentation.algorithms;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import static org.junit.jupiter.api.Assertions.*;

class ManualThresholdAlgorithmTest {

    private final ManualThresholdAlgorithm algorithm =
            new ManualThresholdAlgorithm();

    @BeforeAll
    static void loadOpenCv() {
        OpenCV.loadLocally();
    }

    @Test
    void appliesManualThreshold() {
        Mat input = new Mat(1, 3, CvType.CV_8UC1);
        Mat output = new Mat();

        try {
            input.put(0, 0, 50, 127, 200);

            algorithm.apply(input, output, 127, false);

            assertEquals(0.0, output.get(0, 0)[0]);
            assertEquals(0.0, output.get(0, 1)[0]);
            assertEquals(255.0, output.get(0, 2)[0]);
        } finally {
            input.release();
            output.release();
        }
    }

    @Test
    void appliesInvertedManualThreshold() {
        Mat input = new Mat(1, 3, CvType.CV_8UC1);
        Mat output = new Mat();

        try {
            input.put(0, 0, 50, 127, 200);

            algorithm.apply(input, output, 127, true);

            assertEquals(255.0, output.get(0, 0)[0]);
            assertEquals(255.0, output.get(0, 1)[0]);
            assertEquals(0.0, output.get(0, 2)[0]);
        } finally {
            input.release();
            output.release();
        }
    }

    @Test
    void rejectsInvalidThreshold() {
        Mat input = new Mat(1, 1, CvType.CV_8UC1);
        Mat output = new Mat();

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> algorithm.apply(input, output, -1, false)
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> algorithm.apply(input, output, 256, false)
            );
        } finally {
            input.release();
            output.release();
        }
    }

    @Test
    void rejectsColorImage() {
        Mat input = new Mat(1, 1, CvType.CV_8UC3);
        Mat output = new Mat();

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> algorithm.apply(input, output, 127, false)
            );
        } finally {
            input.release();
            output.release();
        }
    }
}
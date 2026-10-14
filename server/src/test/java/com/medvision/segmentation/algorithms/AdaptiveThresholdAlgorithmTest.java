package com.medvision.segmentation.algorithms;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveThresholdAlgorithmTest {

    private final AdaptiveThresholdAlgorithm algorithm =
            new AdaptiveThresholdAlgorithm();

    @BeforeAll
    static void loadOpenCv() {
        OpenCV.loadLocally();
    }

    @Test
    void producesBinaryMask() {
        Mat input = new Mat(9, 9, CvType.CV_8UC1);
        Mat output = new Mat();

        try {
            input.setTo(new org.opencv.core.Scalar(100));
            input.put(4, 4, 220);

            algorithm.apply(input, output, 3, 5, false);

            assertEquals(input.rows(), output.rows());
            assertEquals(input.cols(), output.cols());
            assertEquals(CvType.CV_8UC1, output.type());

            for (int row = 0; row < output.rows(); row++) {
                for (int col = 0; col < output.cols(); col++) {
                    double value = output.get(row, col)[0];
                    assertTrue(value == 0 || value == 255);
                }
            }
        } finally {
            input.release();
            output.release();
        }
    }

    @Test
    void invertProducesComplementaryMask() {
        Mat input = new Mat(9, 9, CvType.CV_8UC1);
        Mat normal = new Mat();
        Mat inverted = new Mat();
        Mat difference = new Mat();

        try {
            input.setTo(new org.opencv.core.Scalar(100));
            input.put(4, 4, 220);

            algorithm.apply(input, normal, 3, 5, false);
            algorithm.apply(input, inverted, 3, 5, true);

            Core.bitwise_not(normal, difference);

            Mat mismatch = new Mat();
            try {
                Core.compare(difference, inverted, mismatch, Core.CMP_NE);
                assertEquals(0, Core.countNonZero(mismatch));
            } finally {
                mismatch.release();
            }
        } finally {
            input.release();
            normal.release();
            inverted.release();
            difference.release();
        }
    }

    @Test
    void rejectsInvalidBlockSizes() {
        Mat input = new Mat(9, 9, CvType.CV_8UC1);
        Mat output = new Mat();

        try {
            assertThrows(IllegalArgumentException.class,
                    () -> algorithm.apply(input, output, 2, 5, false));

            assertThrows(IllegalArgumentException.class,
                    () -> algorithm.apply(input, output, 32, 5, false));
        } finally {
            input.release();
            output.release();
        }
    }

    @Test
    void rejectsInvalidCValues() {
        Mat input = new Mat(9, 9, CvType.CV_8UC1);
        Mat output = new Mat();

        try {
            assertThrows(IllegalArgumentException.class,
                    () -> algorithm.apply(input, output, 3, Double.NaN, false));

            assertThrows(IllegalArgumentException.class,
                    () -> algorithm.apply(input, output, 3, 51, false));
        } finally {
            input.release();
            output.release();
        }
    }

    @Test
    void rejectsColorImages() {
        Mat input = new Mat(9, 9, CvType.CV_8UC3);
        Mat output = new Mat();

        try {
            assertThrows(IllegalArgumentException.class,
                    () -> algorithm.apply(input, output, 3, 5, false));
        } finally {
            input.release();
            output.release();
        }
    }
}
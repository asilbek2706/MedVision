package com.medvision.segmentation.algorithms;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class AdaptiveThresholdAlgorithm implements SegmentationAlgorithm {

    @Override
    public String code() {
        return "ADAPTIVE";
    }

    @Override
    public void apply(
            Mat grayscale,
            Mat output,
            boolean invert
    ) {
        throw new IllegalArgumentException(
                "Adaptive blockSize and C parameters are required."
        );
    }

    @Override
    public void apply(
            Mat grayscale,
            Mat output,
            AlgorithmParameters parameters
    ) {
        if (parameters == null) {
            throw new IllegalArgumentException(
                    "Algorithm parameters must not be null."
            );
        }

        apply(
                grayscale,
                output,
                parameters.requireBlockSize(),
                parameters.requireC(),
                parameters.invert()
        );
    }

    public void apply(
            Mat grayscale,
            Mat output,
            int blockSize,
            double c,
            boolean invert
    ) {
        if (grayscale == null || grayscale.empty()) {
            throw new IllegalArgumentException(
                    "Grayscale image must not be empty."
            );
        }

        if (output == null) {
            throw new IllegalArgumentException(
                    "Output matrix must not be null."
            );
        }

        if (grayscale.type() != CvType.CV_8UC1) {
            throw new IllegalArgumentException(
                    "Adaptive thresholding requires an 8-bit grayscale image."
            );
        }

        if (blockSize < 3 || blockSize > 31 || blockSize % 2 == 0) {
            throw new IllegalArgumentException(
                    "Block size must be odd and between 3 and 31."
            );
        }

        if (!Double.isFinite(c) || c < -50 || c > 50) {
            throw new IllegalArgumentException(
                    "C must be finite and between -50 and 50."
            );
        }

        int mode = invert
                ? Imgproc.THRESH_BINARY_INV
                : Imgproc.THRESH_BINARY;

        Imgproc.adaptiveThreshold(
                grayscale,
                output,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                mode,
                blockSize,
                c
        );
    }
}
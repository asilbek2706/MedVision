package com.medvision.segmentation.algorithms;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class ManualThresholdAlgorithm implements SegmentationAlgorithm {

    @Override
    public String code() {
        return "MANUAL";
    }

    @Override
    public void apply(
            Mat grayscale,
            Mat output,
            boolean invert
    ) {
        throw new IllegalArgumentException(
                "Manual threshold value is required."
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
                parameters.requireThreshold(),
                parameters.invert()
        );
    }

    public void apply(
            Mat grayscale,
            Mat output,
            int threshold,
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

        if (grayscale.channels() != 1) {
            throw new IllegalArgumentException(
                    "Manual thresholding requires a single-channel image."
            );
        }

        if (threshold < 0 || threshold > 255) {
            throw new IllegalArgumentException(
                    "Threshold must be between 0 and 255."
            );
        }

        int mode = invert
                ? Imgproc.THRESH_BINARY_INV
                : Imgproc.THRESH_BINARY;

        Imgproc.threshold(
                grayscale,
                output,
                threshold,
                255,
                mode
        );
    }
}
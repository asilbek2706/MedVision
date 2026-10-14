package com.medvision.segmentation.algorithms;

import org.opencv.core.Mat;

public interface SegmentationAlgorithm {

    String code();

    void apply(Mat grayscale, Mat output, boolean invert);

    default void apply(
            Mat grayscale,
            Mat output,
            AlgorithmParameters parameters
    ) {
        if (parameters == null) {
            throw new IllegalArgumentException(
                    "Algorithm parameters must not be null."
            );
        }

        apply(grayscale, output, parameters.invert());
    }
}
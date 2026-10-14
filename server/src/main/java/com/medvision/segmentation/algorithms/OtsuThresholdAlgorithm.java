package com.medvision.segmentation.algorithms;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class OtsuThresholdAlgorithm implements SegmentationAlgorithm {

    @Override
    public String code() {
        return "OTSU";
    }

    @Override
    public void apply(Mat grayscale, Mat output, boolean invert) {
        int thresholdMode = invert
                ? Imgproc.THRESH_BINARY_INV
                : Imgproc.THRESH_BINARY;

        Imgproc.threshold(
                grayscale,
                output,
                0,
                255,
                thresholdMode | Imgproc.THRESH_OTSU
        );
    }
}
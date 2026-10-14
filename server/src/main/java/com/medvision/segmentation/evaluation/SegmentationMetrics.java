package com.medvision.segmentation.evaluation;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public final class SegmentationMetrics {

    private SegmentationMetrics() {
    }

    public record Result(
            double dice,
            double iou,
            long truePositive,
            long falsePositive,
            long falseNegative
    ) {
    }

    public static Result calculate(Mat prediction, Mat groundTruth) {
        validate(prediction, groundTruth);

        long tp = 0;
        long fp = 0;
        long fn = 0;

        int rows = prediction.rows();
        int cols = prediction.cols();

        byte[] predictedRow = new byte[cols];
        byte[] truthRow = new byte[cols];

        for (int y = 0; y < rows; y++) {
            prediction.get(y, 0, predictedRow);
            groundTruth.get(y, 0, truthRow);

            for (int x = 0; x < cols; x++) {
                boolean predicted = (predictedRow[x] & 0xFF) != 0;
                boolean actual = (truthRow[x] & 0xFF) != 0;

                if (predicted && actual) {
                    tp++;
                } else if (predicted) {
                    fp++;
                } else if (actual) {
                    fn++;
                }
            }
        }

        long denominator = tp + fp + fn;

        double iou = denominator == 0
                ? 1.0
                : (double) tp / denominator;

        double dice = denominator == 0
                ? 1.0
                : (2.0 * tp) / (2.0 * tp + fp + fn);

        return new Result(dice, iou, tp, fp, fn);
    }

    private static void validate(Mat prediction, Mat groundTruth) {
        if (prediction == null || groundTruth == null) {
            throw new IllegalArgumentException(
                    "Prediction and ground-truth masks are required."
            );
        }

        if (prediction.empty() || groundTruth.empty()) {
            throw new IllegalArgumentException(
                    "Masks must not be empty matrices."
            );
        }

        if (prediction.type() != CvType.CV_8UC1
                || groundTruth.type() != CvType.CV_8UC1) {
            throw new IllegalArgumentException(
                    "Masks must be 8-bit single-channel images."
            );
        }

        if (prediction.rows() != groundTruth.rows()
                || prediction.cols() != groundTruth.cols()) {
            throw new IllegalArgumentException(
                    "Prediction and ground-truth dimensions must match."
            );
        }
    }
}
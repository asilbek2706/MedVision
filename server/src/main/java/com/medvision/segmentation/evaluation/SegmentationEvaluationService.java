package com.medvision.segmentation.evaluation;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;

@Service
public class SegmentationEvaluationService {

    static {
        OpenCV.loadLocally();
    }

    public SegmentationMetrics.Result evaluate(
            byte[] predictedMaskPng,
            byte[] groundTruthMaskPng
    ) {
        if (predictedMaskPng == null
                || predictedMaskPng.length == 0
                || groundTruthMaskPng == null
                || groundTruthMaskPng.length == 0) {
            throw new IllegalArgumentException(
                    "Prediction and ground-truth PNG masks are required."
            );
        }

        Mat prediction = null;
        Mat groundTruth = null;

        try {
            prediction = decode(predictedMaskPng);
            groundTruth = decode(groundTruthMaskPng);

            return SegmentationMetrics.calculate(
                    prediction,
                    groundTruth
            );
        } finally {
            if (prediction != null) {
                prediction.release();
            }

            if (groundTruth != null) {
                groundTruth.release();
            }
        }
    }

    private Mat decode(byte[] pngBytes) {
        MatOfByte buffer = new MatOfByte();

        try {
            buffer.fromArray(pngBytes);

            Mat decoded = Imgcodecs.imdecode(
                    buffer,
                    Imgcodecs.IMREAD_GRAYSCALE
            );

            if (decoded.empty()) {
                decoded.release();

                throw new IllegalArgumentException(
                        "Invalid segmentation mask image."
                );
            }

            return decoded;
        } finally {
            buffer.release();
        }
    }
}
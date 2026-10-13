package com.medvision.segmentation;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class SegmentationService {

    static {
        OpenCV.loadLocally();
    }

    public record Options(
            boolean invert,
            int blurSize,
            int morphologySize,
            int minArea
    ) {
        public Options {
            if (blurSize < 1 || blurSize > 31 || blurSize % 2 == 0) {
                throw new IllegalArgumentException(
                        "Blur size must be odd and between 1 and 31."
                );
            }
            if (morphologySize < 1 || morphologySize > 31
                    || morphologySize % 2 == 0) {
                throw new IllegalArgumentException(
                        "Morphology size must be odd and between 1 and 31."
                );
            }
            if (minArea < 0) {
                throw new IllegalArgumentException(
                        "Minimum area must be non-negative."
                );
            }
        }

        public static Options defaults() {
            return new Options(false, 5, 3, 50);
        }
    }

    public record Result(
            byte[] maskPng,
            byte[] overlayPng,
            long foregroundPixels,
            double foregroundPercent,
            long processingMillis,
            int width,
            int height,
            boolean emptyMask,
            boolean fullMask
    ) {}

    public Result segment(byte[] imageBytes, Options options) {
        if (imageBytes == null || imageBytes.length == 0
                || imageBytes.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Invalid image input.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Options are required.");
        }

        long started = System.nanoTime();

        List<Mat> allocated = new ArrayList<>();

        try {
            MatOfByte encodedInput = new MatOfByte(imageBytes);
            allocated.add(encodedInput);

            Mat original = Imgcodecs.imdecode(
                    encodedInput, Imgcodecs.IMREAD_COLOR
            );
            allocated.add(original);

            if (original.empty()) {
                throw new IllegalArgumentException(
                        "Image could not be decoded."
                );
            }

            int width = original.cols();
            int height = original.rows();

            if (width > 4096 || height > 4096
                    || (long) width * height > 16_000_000L) {
                throw new IllegalArgumentException(
                        "Image dimensions exceed limits."
                );
            }

            Mat gray = new Mat();
            allocated.add(gray);
            Imgproc.cvtColor(
                    original, gray, Imgproc.COLOR_BGR2GRAY
            );

            Mat blurred = new Mat();
            allocated.add(blurred);
            Imgproc.GaussianBlur(
                    gray,
                    blurred,
                    new Size(options.blurSize(), options.blurSize()),
                    0
            );

            Mat mask = new Mat();
            allocated.add(mask);

            int thresholdMode = options.invert()
                    ? Imgproc.THRESH_BINARY_INV
                    : Imgproc.THRESH_BINARY;

            Imgproc.threshold(
                    blurred,
                    mask,
                    0,
                    255,
                    thresholdMode | Imgproc.THRESH_OTSU
            );

            Mat kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_ELLIPSE,
                    new Size(
                            options.morphologySize(),
                            options.morphologySize()
                    )
            );
            allocated.add(kernel);

            Mat cleaned = new Mat();
            allocated.add(cleaned);

            Imgproc.morphologyEx(
                    mask, cleaned, Imgproc.MORPH_OPEN, kernel
            );
            Imgproc.morphologyEx(
                    cleaned, cleaned, Imgproc.MORPH_CLOSE, kernel
            );

            Mat labels = new Mat();
            Mat stats = new Mat();
            Mat centroids = new Mat();

            allocated.add(labels);
            allocated.add(stats);
            allocated.add(centroids);

            int componentCount = Imgproc.connectedComponentsWithStats(
                    cleaned,
                    labels,
                    stats,
                    centroids,
                    8,
                    CvType.CV_32S
            );

            Mat filtered = Mat.zeros(
                    cleaned.size(), CvType.CV_8UC1
            );
            allocated.add(filtered);

            // Component 0 is background.
            for (int i = 1; i < componentCount; i++) {
                int area = (int) stats.get(
                        i, Imgproc.CC_STAT_AREA
                )[0];

                if (area >= options.minArea()) {
                    Mat region = new Mat();
                    allocated.add(region);
                    Core.compare(
                            labels,
                            new Scalar(i),
                            region,
                            Core.CMP_EQ
                    );
                    filtered.setTo(new Scalar(255), region);
                    region.release();
                    allocated.remove(region);
                }
            }

            long foreground = Core.countNonZero(filtered);
            long totalPixels = (long) width * height;

            Mat overlay = original.clone();
            allocated.add(overlay);

            Mat colored = new Mat(
                    original.size(),
                    original.type(),
                    new Scalar(0, 0, 255)
            );
            allocated.add(colored);

            Mat blended = new Mat();
            allocated.add(blended);

            Core.addWeighted(
                    original, 0.6,
                    colored, 0.4,
                    0,
                    blended
            );

            blended.copyTo(overlay, filtered);

            byte[] maskPng = encodePng(filtered);
            byte[] overlayPng = encodePng(overlay);

            return new Result(
                    maskPng,
                    overlayPng,
                    foreground,
                    foreground * 100.0 / totalPixels,
                    (System.nanoTime() - started) / 1_000_000,
                    width,
                    height,
                    foreground == 0,
                    foreground == totalPixels
            );
        } finally {
            for (int i = allocated.size() - 1; i >= 0; i--) {
                allocated.get(i).release();
            }
        }
    }

    private static byte[] encodePng(Mat image) {
        MatOfByte buffer = new MatOfByte();
        try {
            if (!Imgcodecs.imencode(".png", image, buffer)) {
                throw new IllegalStateException(
                        "PNG encoding failed."
                );
            }
            return buffer.toArray();
        } finally {
            buffer.release();
        }
    }
}
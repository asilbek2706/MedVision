package com.medvision.segmentation.algorithms;

public record AlgorithmParameters(
        boolean invert,
        Integer threshold,
        Integer blockSize,
        Double c
) {

    public AlgorithmParameters {
        if (threshold != null && (threshold < 0 || threshold > 255)) {
            throw new IllegalArgumentException(
                    "Threshold must be between 0 and 255."
            );
        }

        if (blockSize != null &&
                (blockSize < 3 || blockSize > 31 || blockSize % 2 == 0)) {
            throw new IllegalArgumentException(
                    "Block size must be odd and between 3 and 31."
            );
        }

        if (c != null &&
                (!Double.isFinite(c) || c < -50 || c > 50)) {
            throw new IllegalArgumentException(
                    "C must be finite and between -50 and 50."
            );
        }
    }

    public static AlgorithmParameters otsu(boolean invert) {
        return new AlgorithmParameters(invert, null, null, null);
    }

    public static AlgorithmParameters manual(
            boolean invert,
            int threshold
    ) {
        return new AlgorithmParameters(invert, threshold, null, null);
    }

    public static AlgorithmParameters adaptive(
            boolean invert,
            int blockSize,
            double c
    ) {
        return new AlgorithmParameters(invert, null, blockSize, c);
    }

    public int requireThreshold() {
        if (threshold == null) {
            throw new IllegalArgumentException(
                    "Manual threshold value is required."
            );
        }
        return threshold;
    }

    public int requireBlockSize() {
        if (blockSize == null) {
            throw new IllegalArgumentException(
                    "Adaptive block size is required."
            );
        }
        return blockSize;
    }

    public double requireC() {
        if (c == null) {
            throw new IllegalArgumentException(
                    "Adaptive C value is required."
            );
        }
        return c;
    }
}
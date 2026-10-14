package com.medvision.segmentation.algorithms;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AlgorithmRegistry {

    private final Map<String, SegmentationAlgorithm> algorithms;

    public AlgorithmRegistry(List<SegmentationAlgorithm> implementations) {
        Map<String, SegmentationAlgorithm> registered = new HashMap<>();

        for (SegmentationAlgorithm algorithm : implementations) {
            String code = algorithm.code();

            if (code == null || code.isBlank()) {
                throw new IllegalStateException(
                        "Algorithm code must not be blank."
                );
            }

            if (registered.putIfAbsent(code, algorithm) != null) {
                throw new IllegalStateException(
                        "Duplicate algorithm code: " + code
                );
            }
        }

        this.algorithms = Map.copyOf(registered);
    }

    public SegmentationAlgorithm get(String code) {
        SegmentationAlgorithm algorithm = algorithms.get(code);

        if (algorithm == null) {
            throw new IllegalArgumentException(
                    "Unsupported segmentation algorithm: " + code
            );
        }

        return algorithm;
    }

    public Set<String> supportedAlgorithms() {
        return algorithms.keySet();
    }
}
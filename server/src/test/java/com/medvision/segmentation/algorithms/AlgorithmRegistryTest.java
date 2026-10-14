package com.medvision.segmentation.algorithms;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AlgorithmRegistryTest {

    @ComponentScan(basePackageClasses = AlgorithmRegistry.class)
    static class AlgorithmConfiguration {
    }

    @Test
    void springRegistersAllThreeAlgorithms() {
        try (var context = new AnnotationConfigApplicationContext(
                AlgorithmConfiguration.class
        )) {
            AlgorithmRegistry registry =
                    context.getBean(AlgorithmRegistry.class);

            assertEquals(
                    Set.of("OTSU", "MANUAL", "ADAPTIVE"),
                    registry.supportedAlgorithms()
            );

            assertInstanceOf(
                    OtsuThresholdAlgorithm.class,
                    registry.get("OTSU")
            );

            assertInstanceOf(
                    ManualThresholdAlgorithm.class,
                    registry.get("MANUAL")
            );

            assertInstanceOf(
                    AdaptiveThresholdAlgorithm.class,
                    registry.get("ADAPTIVE")
            );
        }
    }

    @Test
    void rejectsUnknownAlgorithm() {
        AlgorithmRegistry registry = new AlgorithmRegistry(
                List.of(new OtsuThresholdAlgorithm())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.get("UNKNOWN")
        );
    }

    @Test
    void rejectsDuplicateAlgorithmCodes() {
        assertThrows(
                IllegalStateException.class,
                () -> new AlgorithmRegistry(
                        List.of(
                                new OtsuThresholdAlgorithm(),
                                new OtsuThresholdAlgorithm()
                        )
                )
        );
    }
}
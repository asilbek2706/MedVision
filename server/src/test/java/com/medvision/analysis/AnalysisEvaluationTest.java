package com.medvision.analysis;

import com.medvision.image.MedicalImage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisEvaluationTest {

    private MedicalImage createMedicalImage() {
        try {
            var constructor =
                    MedicalImage.class.getDeclaredConstructor();

            constructor.setAccessible(true);

            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "Unable to create test MedicalImage.",
                    ex
            );
        }
    }
    private Analysis completedAnalysis() throws Exception {
        MedicalImage image = createMedicalImage();

        Field imageId = MedicalImage.class.getDeclaredField("id");
        imageId.setAccessible(true);
        imageId.set(image, UUID.randomUUID());

        Field width = MedicalImage.class.getDeclaredField("width");
        width.setAccessible(true);
        width.set(image, 10);

        Field height = MedicalImage.class.getDeclaredField("height");
        height.setAccessible(true);
        height.set(image, 10);

        Analysis analysis = new Analysis(
                image,
                "OTSU",
                "Evaluation test",
                Map.of("invert", false)
        );

        analysis.complete(
                "masks/test.png",
                "overlays/test.png",
                50,
                0.5,
                12
        );

        return analysis;
    }

    @Test
    void metricsAreNullWithoutGroundTruth() throws Exception {
        Analysis analysis = completedAnalysis();

        assertNull(analysis.getDiceScore());
        assertNull(analysis.getIouScore());
        assertNull(analysis.getGroundTruthKey());

        AnalysisResponse response = AnalysisResponse.from(analysis);

        assertNull(response.diceScore());
        assertNull(response.iouScore());
        assertFalse(response.hasGroundTruth());
    }

    @Test
    void storesValidEvaluationResults() throws Exception {
        Analysis analysis = completedAnalysis();

        analysis.completeEvaluation("ground-truth/test.png", 0.85, 0.74);

        assertEquals(0.85, analysis.getDiceScore(), 0.000001);
        assertEquals(0.74, analysis.getIouScore(), 0.000001);
        assertEquals("ground-truth/test.png", analysis.getGroundTruthKey());

        AnalysisResponse response = AnalysisResponse.from(analysis);

        assertEquals(0.85, response.diceScore(), 0.000001);
        assertEquals(0.74, response.iouScore(), 0.000001);
        assertTrue(response.hasGroundTruth());
    }

    @Test
    void rejectsDuplicateEvaluation() throws Exception {
        Analysis analysis = completedAnalysis();

        analysis.completeEvaluation("ground-truth/first.png", 0.9, 0.8);

        assertThrows(
                IllegalStateException.class,
                () -> analysis.completeEvaluation(
                        "ground-truth/second.png", 0.7, 0.6
                )
        );
    }

    @Test
    void rejectsInvalidScores() throws Exception {
        Analysis analysis = completedAnalysis();

        assertThrows(
                IllegalArgumentException.class,
                () -> analysis.completeEvaluation(
                        "ground-truth/test.png", -0.1, 0.5
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> analysis.completeEvaluation(
                        "ground-truth/test.png", 0.8, Double.NaN
                )
        );

        assertNull(analysis.getGroundTruthKey());
    }

    @Test
    void rejectsEvaluationBeforeAnalysisCompletion() {
        MedicalImage image = createMedicalImage();

        Analysis analysis = new Analysis(
                image,
                "OTSU",
                "Processing test",
                Map.of("invert", false)
        );

        assertThrows(
                IllegalStateException.class,
                () -> analysis.completeEvaluation(
                        "ground-truth/test.png", 1.0, 1.0
                )
        );
    }
}
package com.medvision.segmentation;

import nu.pattern.OpenCV;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OpenCvSmokeTest {

    @Test
    void loadsNativeLibraryAndReadsPng() throws Exception {
        OpenCV.loadLocally();

        Path png = Files.createTempFile(
                "medvision-opencv-", ".png"
        );

        Mat original = new Mat();
        Mat decoded = new Mat();

        try {
            original.create(32, 48, CvType.CV_8UC3);
            original.setTo(
                    new org.opencv.core.Scalar(40, 100, 180)
            );

            assertTrue(
                    Imgcodecs.imwrite(png.toString(), original),
                    "PNG writing failed"
            );

            decoded.release();
            decoded = Imgcodecs.imread(
                    png.toString(),
                    Imgcodecs.IMREAD_COLOR
            );

            assertFalse(decoded.empty(), "PNG reading failed");
            assertEquals(48, decoded.cols());
            assertEquals(32, decoded.rows());
            assertEquals(3, decoded.channels());

            double[] pixel = decoded.get(0, 0);
            assertNotNull(pixel);
            assertEquals(40.0, pixel[0], 0.01);
            assertEquals(100.0, pixel[1], 0.01);
            assertEquals(180.0, pixel[2], 0.01);

            System.out.println(
                    "[PASS] OpenCV native loaded; PNG decoded: "
                    + decoded.cols() + "x" + decoded.rows()
            );
        } finally {
            decoded.release();
            original.release();
            Files.deleteIfExists(png);
        }
    }
}

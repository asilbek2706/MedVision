# MedVision Limitations and Future Work

**Official Project:** Image Segmentation for Medical Image Analysis

## 1. Project Scope

MedVision is an educational application demonstrating classical threshold-based image segmentation.
It is not a certified medical device or clinical decision-support system.

## 2. Algorithm Limitations

### Otsu Thresholding

Otsu uses a global threshold and may perform poorly when foreground and background intensities overlap.

### Manual Thresholding

Manual thresholding requires the user to choose an appropriate threshold.
Its results can vary substantially with parameter selection.

### Adaptive Thresholding

Adaptive thresholding calculates local thresholds and can help with uneven intensity distributions.
It is sensitive to neighborhood size and offset parameters.

## 3. Input Limitations

The application currently supports PNG and JPEG image uploads.
Native DICOM workflows and volumetric medical image processing are outside the documented scope.

## 4. Evaluation Limitations

Dice and IoU depend on the availability and quality of ground-truth masks.
High overlap scores alone do not demonstrate clinical safety or diagnostic accuracy.
No benchmark performance results are claimed without actual experimental measurements.

## 5. Dataset Limitations

No external medical imaging dataset has been verified in the repository.
Existing test fixtures primarily support software validation.
Clinical generalization has not been established.

## 6. Security and Privacy

The application includes authentication and resource access controls.
These features do not by themselves establish compliance with healthcare privacy regulations.
Sensitive patient information must not be used in public demonstrations.

## 7. Future Improvements

- Integrate a properly licensed medical image dataset.
- Establish a reproducible evaluation benchmark.
- Compare segmentation methods on multiple annotated images.
- Investigate additional segmentation techniques.
- Explore DICOM support where appropriate.
- Perform further security, performance, and usability testing.

## 8. Conclusion

MedVision demonstrates a functional academic workflow for image upload, segmentation, result visualization, and optional evaluation.
Its outputs are experimental and must not be used for medical diagnosis.

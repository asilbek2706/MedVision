# MedVision Algorithm Comparison

**Project:** Image Segmentation for Medical Image Analysis

## 1. Overview

MedVision supports three threshold-based segmentation methods implemented using OpenCV.

## 2. Qualitative Comparison

| Feature | OTSU | MANUAL | ADAPTIVE |
|---|---|---|---|
| Threshold selection | Automatic global | User-defined global | Local adaptive |
| User threshold required | No | Yes | No |
| Local intensity variation | Limited handling | Limited handling | Better suited |
| Parameter sensitivity | Blur and inversion | Threshold, blur and inversion | Block size, C, blur and inversion |
| Typical strength | Simple automatic thresholding | Direct user control | Uneven illumination |
| Typical limitation | Overlapping intensity distributions | Requires threshold tuning | Sensitive to local parameters |

## 3. Evaluation Metrics

Dice = 2 * TP / (2 * TP + FP + FN)

IoU = TP / (TP + FP + FN)

TP: True Positive pixels.
FP: False Positive pixels.
FN: False Negative pixels.

Higher Dice and IoU values indicate stronger overlap with the supplied ground-truth mask.

## 4. Fair Comparison Procedure

1. Upload one test image.
2. Execute at least two algorithms on the same image.
3. Save the masks and overlays.
4. Evaluate each result against the same ground-truth mask.
5. Record Dice, IoU, and processing time.
6. Compare both numerical metrics and visual quality.

## 5. Results Table Template

| Image | Algorithm | Dice | IoU | Processing Time (ms) |
|---|---|---|---|---|
| Test Image 1 | OTSU | Not measured | Not measured | Not measured |
| Test Image 1 | MANUAL | Not measured | Not measured | Not measured |
| Test Image 1 | ADAPTIVE | Not measured | Not measured | Not measured |

No experimental scores are claimed until actual measurements are collected.

## 6. Conclusion

There is no universally best thresholding algorithm for every medical image.
The preferred method depends on image characteristics, parameter selection, and evaluation results.

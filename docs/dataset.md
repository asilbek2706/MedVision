# MedVision Dataset Documentation

**Official Project:** Image Segmentation for Medical Image Analysis

## 1. Dataset Status

No external medical imaging dataset has been verified for this project.
The repository currently contains small test fixtures and generated API response images.
These files must not be represented as a validated clinical dataset.

## 2. Existing Test Fixtures

| File | Purpose |
|---|---|
| server/requests/fixtures/valid.png | PNG upload validation |
| server/requests/fixtures/valid.jpg | JPEG upload validation |
| server/requests/fixtures/fake.png | Invalid image rejection testing |
| server/requests/fixtures/large.png | File size limit testing |

The repository also contains generated mask and overlay responses in the local IntelliJ HTTP request cache.
These files are development artifacts rather than an independently sourced dataset.

## 3. Dataset Source

External dataset: Not selected or verified.
Dataset publisher: Not applicable at the current stage.
Dataset download URL: Not available.
Dataset version: Not available.
Dataset license: Not verified.

## 4. Data Usage Policy

Only authorized, non-sensitive images should be used for demonstrations.
Do not upload identifiable patient images without appropriate authorization.
A future external dataset must have its source, version, license, and usage restrictions documented before inclusion.

## 5. Evaluation Requirements

Quantitative segmentation evaluation requires image-specific ground-truth masks.
Dice and IoU scores should only be reported when the corresponding predictions and reference masks have actually been evaluated.

## 6. Future Dataset Integration

1. Select a suitable public medical imaging dataset.
2. Verify its publisher and original download page.
3. Review the dataset license and permitted uses.
4. Document modality, image count, resolution, and annotation format.
5. Prepare reproducible image and mask pairs.
6. Record experimental parameters and actual evaluation results.

# MedVision REST API Documentation

Official Project: Image Segmentation for Medical Image Analysis

## API Overview

Backend: http://localhost:18080
Authentication: Session cookie and CSRF

## API Endpoints

### Health

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/health | Check backend status. Access: public. |

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/auth/csrf | Retrieve CSRF token. Access: public. |
| POST | /api/auth/register | Register user. Access: public. |
| POST | /api/auth/login | Log in and establish session. Access: public. |
| GET | /api/auth/me | Get current user. Access: protected. |
| POST | /api/auth/logout | Invalidate session. Access: protected. |

### Images

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/images | Upload PNG or JPEG image. Access: protected. |
| GET | /api/images?page=0&size=20 | List owned images. Access: protected. |
| GET | /api/images/{{imageId}} | Get image metadata. Access: protected. |
| GET | /api/images/{{imageId}}/content | Get original image. Access: protected. |
| DELETE | /api/images/{{imageId}} | Delete owned image. Access: protected. |

### Segmentation

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/analysis-options | Get algorithm options. Access: protected. |
| POST | /api/analysis-options/validate | Validate segmentation parameters. Access: protected. |
| POST | /api/images/{{imageId}}/analyses | Execute Otsu segmentation. Access: protected. |
| POST | /api/images/{{imageId}}/analyses | Execute Manual segmentation. Access: protected. |
| POST | /api/images/{{imageId}}/analyses | Execute Adaptive segmentation. Access: protected. |

### Analyses

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/analyses?page=0&size=20 | List analyses with optional filters. Access: protected. |
| GET | /api/images/{{imageId}}/analyses?page=0&size=20 | List analyses for an image. Access: protected. |
| GET | /api/analyses/{{analysisId}} | Get analysis details. Access: protected. |
| PATCH | /api/analyses/{{analysisId}} | Rename analysis. Access: protected. |

### Evaluation

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/analyses/{{analysisId}}/evaluate | Evaluate against ground truth. Access: protected. |

### Results

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/analyses/{{analysisId}}/mask | View segmentation mask. Access: protected. |
| GET | /api/analyses/{{analysisId}}/overlay | View segmentation overlay. Access: protected. |
| GET | /api/analyses/{{analysisId}}/mask/download | Download mask PNG. Access: protected. |
| GET | /api/analyses/{{analysisId}}/overlay/download | Download overlay PNG. Access: protected. |

### Dashboard

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/dashboard/stats | Get user dashboard statistics. Access: protected. |

## Segmentation Algorithms

OTSU: Automatic global thresholding.
MANUAL: User-defined global thresholding.
ADAPTIVE: Local thresholding.

## Evaluation

Dice and IoU compare segmentation masks with ground truth.

## Security

Protected requests require authentication.
State-changing requests require CSRF protection.

## Postman Collection

Import postman/MedVision.postman_collection.json.

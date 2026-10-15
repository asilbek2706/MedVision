# Image Segmentation for Medical Image Analysis

**MedVision — Medical Image Intelligence**

A full-stack web application for medical image segmentation, visualization, evaluation, and analysis management.

> **Project type:** Academic software engineering project  
> **Application:** MedVision  
> **Technology stack:** Next.js, React, TypeScript, Java, Spring Boot, OpenCV, PostgreSQL, Docker

---

## 1. Introduction

Medical image segmentation is the process of dividing a digital medical image into meaningful regions. It can help researchers and developers examine image structures, identify regions of interest, and study image-processing techniques.

**Image Segmentation for Medical Image Analysis** is an academic project implemented through a web application named **MedVision**.

MedVision provides a user-friendly environment for uploading images, configuring segmentation algorithms, visualizing processed results, and comparing different methods.

### Problem Statement

Manual image segmentation can be time-consuming and inconsistent. Different image characteristics, including brightness, contrast, and noise, can also affect segmentation quality.

This project explores conventional threshold-based segmentation methods and provides an interface for studying their behavior.

### Proposed Solution

MedVision integrates image-processing algorithms with a modern full-stack application.

The application supports:

- User registration and login.
- PNG and JPEG image uploads.
- Image validation and secure file storage.
- Otsu thresholding.
- Manual thresholding.
- Adaptive thresholding.
- Segmentation mask and overlay visualization.
- Analysis history, filtering, and renaming.
- Downloadable segmentation results.
- Ground-truth evaluation using Dice and IoU metrics.

**Important:** MedVision is an educational and experimental image-analysis system. It is not a certified medical device and must not be used as a standalone clinical diagnostic tool.

---

## 2. Learning Objective of the Project

The primary objective is to understand and implement medical image segmentation through a practical full-stack software project.

### Learning Objectives

1. Understand the fundamental concepts of digital image processing and segmentation.
2. Study and implement conventional threshold-based segmentation algorithms.
3. Compare the behavior of different segmentation methods.
4. Learn how to evaluate segmentation masks using reference ground-truth images.
5. Develop REST APIs using Java and Spring Boot.
6. Build an interactive frontend using Next.js, React, and TypeScript.
7. Store application metadata and analysis results using PostgreSQL.
8. Apply authentication, authorization, validation, and secure storage practices.
9. Use Docker for reproducible local development.
10. Practice software testing, debugging, documentation, and project presentation.

### Expected Learning Outcomes

By completing this project, the developer gains practical experience in:

- Image-processing workflows.
- Full-stack application architecture.
- Database modeling and migrations.
- REST API design.
- Algorithm implementation and evaluation.
- Software security and testing.
- Technical documentation and demonstration.

---

## 3. Tools and Technologies Used

### Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Frontend | Next.js 16.4 | Web application framework |
| User interface | React 19 | Interactive components |
| Programming language | TypeScript | Type-safe frontend development |
| Styling | Tailwind CSS 4 | Responsive application design |
| Icons | Lucide React | Interface icons |
| Backend | Java 21 | Server-side programming |
| Backend framework | Spring Boot 4.1.1 | REST API and application services |
| Image processing | OpenCV 4.9 | Segmentation operations |
| Security | Spring Security | Authentication and access control |
| Persistence | Spring Data JPA | Database access |
| Database | PostgreSQL 17 | Persistent application metadata |
| Database migration | Flyway | Version-controlled database schema |
| Build tools | Maven and npm | Dependency and build management |
| Development environment | Docker Compose | Local service orchestration |
| Version control | Git and GitHub | Source code management |

### System Architecture

MedVision uses a client-server architecture.

```text
                 USER / WEB BROWSER
                          |
                          v
              NEXT.JS / REACT FRONTEND
                   localhost:3000
                          |
                     /api/* proxy
                          |
                          v
              SPRING BOOT REST API
                   localhost:18080
                          |
             +------------+-------------+
             |            |             |
             v            v             v
        PostgreSQL     OpenCV        File Storage
        Database     Segmentation    Images / Masks
             |            |             |
             +------------+-------------+
                          |
                          v
                ANALYSIS RESULTS
                History / Download
```

### Database Overview

The main database entities include:

- **users:** Registered application users.
- **medical_images:** Uploaded image metadata and ownership.
- **analyses:** Segmentation configuration, processing status, results, and evaluation scores.
- **storage_deletion_jobs:** Pending storage cleanup operations.

Core relationships:

```text
users (1) -------- (N) medical_images
medical_images (1) -------- (N) analyses
```

Database changes are managed through versioned Flyway SQL migrations.

### Installation and Environment Setup

#### Prerequisites

- Git
- Docker Desktop with Docker Compose
- A modern web browser

Java 21, Maven, and Node.js are also useful for development outside Docker.

#### Clone the Repository

```powershell
git clone https://github.com/asilbek2706/MedVision.git
cd MedVision
```

For the documented development branch:

```powershell
git checkout feature/phase-12-security-stability
```

#### Start the Application

Make sure Docker Desktop is running.

```powershell
docker compose up -d --build
```

Check service status:

```powershell
docker compose ps
```

View application logs:

```powershell
docker compose logs -f backend frontend
```

#### Local Development URLs

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend health | http://localhost:18080/api/health |
| Health through frontend | http://localhost:3000/api/health |
| PostgreSQL | localhost:5433 |

#### Environment Configuration

The development Docker Compose configuration provides database connection settings to the backend.

The main backend configuration variables are:

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `SESSION_COOKIE_SECURE` | Secure session-cookie setting |

The Next.js application uses `BACKEND_URL` to proxy `/api/*` requests to Spring Boot.

Default development settings are intended for local use only. Production deployment requires secure credentials, HTTPS, and an appropriate environment configuration.

#### Useful Development Commands

Backend tests:

```powershell
docker compose exec backend ./mvnw test
```

Frontend TypeScript validation:

```powershell
docker compose exec frontend npx tsc --noEmit
```

Frontend linting:

```powershell
docker compose exec frontend npm run lint
```

Stop services:

```powershell
docker compose stop
```

**Warning:** `docker compose down -v` deletes Docker-managed volumes, including persistent database data.

---

## 4. Explain Learned Methodology

### 4.1 Development Methodology

The project was developed incrementally, with separate implementation stages for backend services, image storage, segmentation algorithms, frontend interfaces, security, testing, and documentation.

The general workflow includes:

1. Requirements analysis.
2. System and database design.
3. Backend API implementation.
4. Image upload and storage integration.
5. Segmentation algorithm development.
6. Frontend implementation.
7. Evaluation and testing.
8. Security improvements.
9. Documentation and demonstration.

### 4.2 Image Segmentation Pipeline

The segmentation workflow follows these steps:

```text
User Login
    |
    v
Image Upload (PNG / JPEG)
    |
    v
File Validation
    |
    v
Image Decoding
    |
    v
Algorithm Selection
    |
    +---- Otsu Thresholding
    |
    +---- Manual Thresholding
    |
    +---- Adaptive Thresholding
    |
    v
Segmentation Processing
    |
    v
Binary Mask Generation
    |
    v
Overlay Visualization
    |
    v
Store Analysis Metadata and Results
    |
    v
History / Comparison / Download
```

### 4.3 Implemented Segmentation Algorithms

#### Otsu Thresholding

Otsu's method automatically selects a global threshold by analyzing the image intensity distribution.

It aims to separate image pixels into two classes using a threshold that minimizes within-class variance.

**Advantages:**
- Automatic threshold selection.
- Simple configuration.
- Suitable for some images with distinguishable intensity groups.

**Limitations:**
- Sensitive to uneven illumination.
- May perform poorly when foreground and background intensities overlap.

#### Manual Thresholding

Manual thresholding uses a threshold value selected by the user.

Pixels are classified according to their intensity relative to the chosen threshold.

**Advantages:**
- Simple and understandable.
- Gives the user direct control.
- Useful for demonstrating threshold effects.

**Limitations:**
- Requires manual parameter selection.
- A single global threshold may not handle varying illumination.

#### Adaptive Thresholding

Adaptive thresholding calculates local thresholds from image neighborhoods instead of relying on one global threshold.

**Advantages:**
- Can handle nonuniform illumination better than global thresholding.
- Uses local image characteristics.

**Limitations:**
- Requires additional parameter configuration.
- Can be sensitive to noise and local texture.

### 4.4 Algorithm Comparison

| Criterion | Otsu | Manual | Adaptive |
|---|---|---|---|
| Threshold selection | Automatic | User-defined | Local adaptive |
| Global threshold | Yes | Yes | No |
| Parameter tuning | Low | Required | Required |
| Uneven illumination | Limited | Limited | Often better |
| Main advantage | Automatic threshold | User control | Local adaptation |
| Main limitation | Histogram dependence | Manual tuning | Noise sensitivity |

This table compares algorithm characteristics conceptually. It does not represent measured accuracy or performance rankings.

### 4.5 Segmentation Evaluation

MedVision supports evaluating a predicted segmentation mask against a ground-truth mask.

Two evaluation metrics are used:

#### Dice Similarity Coefficient

```text
Dice = 2 * |A intersection B| / (|A| + |B|)
```

#### Intersection over Union (IoU)

```text
IoU = |A intersection B| / |A union B|
```

Where:

- `A` is the predicted segmentation region.
- `B` is the ground-truth segmentation region.

Both metrics are commonly represented between 0 and 1, with higher values indicating greater overlap.

The interpretation of scores depends on the quality and relevance of the ground-truth masks.

### 4.6 REST API Overview

The backend exposes REST endpoints for authentication, image management, segmentation, evaluation, and analysis history.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Backend health status |
| GET | `/api/auth/csrf` | Retrieve CSRF token |
| POST | `/api/auth/register` | Register a user |
| POST | `/api/auth/login` | Authenticate a user |
| GET | `/api/auth/me` | Retrieve current user |
| POST | `/api/images` | Upload an image |
| GET | `/api/images` | List owned images |
| GET | `/api/images/{id}` | Retrieve image metadata |
| GET | `/api/images/{id}/content` | Retrieve image content |
| DELETE | `/api/images/{id}` | Delete an owned image |
| GET | `/api/analysis-options` | Retrieve algorithm options |
| POST | `/api/analysis-options/validate` | Validate analysis parameters |
| POST | `/api/images/{imageId}/analyses` | Execute segmentation |
| GET | `/api/analyses` | List analyses |
| GET | `/api/analyses/{id}` | Retrieve analysis details |
| PATCH | `/api/analyses/{id}` | Rename an analysis |
| POST | `/api/analyses/{id}/evaluate` | Evaluate against ground truth |
| GET | `/api/analyses/{id}/mask` | Retrieve segmentation mask |
| GET | `/api/analyses/{id}/overlay` | Retrieve overlay |
| GET | `/api/analyses/{id}/mask/download` | Download mask |
| GET | `/api/analyses/{id}/overlay/download` | Download overlay |
| GET | `/api/dashboard/stats` | Retrieve dashboard statistics |

Protected endpoints require an authenticated session. State-changing requests may also require a CSRF token.

Additional endpoint examples are available in the `server/requests/` directory.

### 4.7 Security and Validation

The project incorporates:

- Session-based authentication.
- Spring Security authorization.
- CSRF protection.
- Owner-based resource access.
- Login attempt limiting.
- Upload file-type and size validation.
- Image decoding validation.
- Restricted image dimensions.
- Controlled storage identifiers.
- Structured API error responses.

Uploaded images are limited to supported PNG and JPEG formats. The database schema enforces a maximum image size of 10 MiB and maximum width and height of 4096 pixels.

### 4.8 Testing

The project includes backend tests for:

- Authentication and login limiting.
- Image upload validation.
- Storage behavior.
- Ownership and authorization.
- Segmentation algorithms.
- Analysis execution.
- Evaluation metrics.
- Database mappings.
- API error handling.

Frontend validation uses TypeScript, ESLint, and the Next.js production build.

### 4.9 Demonstration Workflow

The intended project demonstration follows this sequence:

1. Open MedVision.
2. Register or log in.
3. Upload a PNG or JPEG image.
4. Select the first segmentation algorithm.
5. Execute segmentation and inspect the mask and overlay.
6. Repeat with a second algorithm.
7. Compare the segmentation outputs.
8. Optionally evaluate results using ground-truth masks.
9. Open the analysis history.
10. Download the segmentation result.

---

## 5. Conclusion of the Learned Topic or Project

### Project Summary

MedVision demonstrates how conventional image segmentation techniques can be integrated into a full-stack web application.

The project combines frontend development, backend services, database management, file storage, security, and digital image processing.

It provides a practical environment for learning how different threshold-based segmentation algorithms behave on image data.

### Main Achievements

- Implemented a web-based segmentation workflow.
- Integrated three conventional thresholding algorithms.
- Added segmentation mask and overlay visualization.
- Implemented analysis history and result downloads.
- Added ground-truth evaluation using Dice and IoU.
- Designed a PostgreSQL database with versioned migrations.
- Applied authentication, ownership checks, and input validation.
- Established a Docker-based development environment.

### Project Limitations

- Threshold-based methods are sensitive to image characteristics.
- Segmentation quality may vary across different image types.
- No validated clinical performance is claimed.
- The system is not intended for medical diagnosis.
- Dataset-specific quantitative comparisons require suitable ground-truth data.
- The application currently focuses on conventional segmentation rather than trained deep-learning models.

### Future Improvements

Possible extensions include:

- Additional segmentation algorithms.
- Support for specialized medical image formats.
- Deep-learning-based segmentation.
- More comprehensive quantitative evaluation.
- Improved experiment tracking.
- Deployment and production monitoring.

### Final Statement

**Image Segmentation for Medical Image Analysis** demonstrates the practical application of software engineering and digital image processing concepts through MedVision.

The project provides a foundation for further research, experimentation, and development in medical image analysis.

---

**Repository:** https://github.com/asilbek2706/MedVision

**Project:** Image Segmentation for Medical Image Analysis  
**Application:** MedVision  
**Purpose:** Academic learning and demonstration
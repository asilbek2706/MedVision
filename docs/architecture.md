# MedVision — System Architecture

**Official project:** Image Segmentation for Medical Image Analysis

## Architecture Diagram

```mermaid
flowchart TD
    U["User / Web Browser"]
    F["Next.js 16 + React 19 Frontend"]
    A["Spring Boot REST API"]
    S["Spring Security / Sessions"]
    I["Image Upload and Validation"]
    P["OpenCV Segmentation Engine"]
    E["Dice / IoU Evaluation"]
    DB[("PostgreSQL 17")]
    FS[("Image and Result Storage")]

    U --> F
    F -->|"HTTP /api"| A
    A --> S
    A --> I
    A --> P
    A --> E
    A --> DB
    I --> FS
    P --> FS
    E --> DB
    P --> DB
```

## Component Responsibilities

| Component | Responsibility |
|---|---|
| Next.js frontend | User interface, upload forms, visualization, history |
| Spring Boot API | Business logic, authentication, request validation |
| Spring Security | Sessions, authorization, CSRF protection |
| OpenCV | Otsu, Manual and Adaptive segmentation |
| PostgreSQL | Users, images, analyses and metadata |
| File storage | Uploaded images, generated masks and overlays |
| Evaluation module | Dice and IoU calculation |

## Segmentation Workflow

```mermaid
flowchart TD
    A["Login"] --> B["Upload PNG / JPEG"]
    B --> C["Validate and Decode Image"]
    C --> D{"Select Algorithm"}
    D --> E["Otsu"]
    D --> F["Manual"]
    D --> G["Adaptive"]
    E --> H["Generate Binary Mask"]
    F --> H
    G --> H
    H --> I["Generate Overlay"]
    I --> J["Save Analysis"]
    J --> K["History / Comparison / Download"]
```

> MedVision is intended for educational and experimental use, not clinical diagnosis.
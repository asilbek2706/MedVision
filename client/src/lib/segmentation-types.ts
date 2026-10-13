export type MedicalImage = {
  id: string;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  width: number;
  height: number;
  createdAt: string;
};

export type AnalysisOptions = {
  plannedAlgorithms: string[];
  acceptedFormats: string[];
  maxFileSizeBytes: number;
  maxWidth: number;
  maxHeight: number;
  segmentationAvailable: boolean;
};

export type AnalysisParameters = {
  algorithm: "OTSU";
  blurKernelSize: number;
  invert: boolean;
};

export type AnalysisStatus =
  | "PENDING"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED";

export type AnalysisResult = {
  id: string;
  imageId: string;
  algorithm: string;
  parameters: Record<string, unknown>;
  status: AnalysisStatus;
  foregroundPixels: number | null;
  foregroundRatio: number | null;
  processingTimeMs: number | null;
  errorCode: string | null;
  createdAt: string;
  completedAt: string | null;
};

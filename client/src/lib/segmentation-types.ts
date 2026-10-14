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
  name?: string;
  algorithm: "OTSU" | "MANUAL" | "ADAPTIVE";
  blurKernelSize: number;
  invert: boolean;
  threshold?: number;
  blockSize?: number;
  c?: number;
};

export type AnalysisStatus =
  | "PENDING"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED";

export type AnalysisResult = {
  id: string;
  name: string;
  imageId: string;
  algorithm: string;
  parameters: Record<string, unknown>;
  status: AnalysisStatus;
  foregroundPixels: number | null;
  foregroundRatio: number | null;
  processingTimeMs: number | null;
  diceScore: number | null;
  iouScore: number | null;
  hasGroundTruth: boolean;
  errorCode: string | null;
  createdAt: string;
  completedAt: string | null;
};

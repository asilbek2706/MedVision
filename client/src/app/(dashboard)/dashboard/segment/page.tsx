"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ScanLine, ShieldCheck, LayoutDashboard, History, Activity } from "lucide-react";

import { MedVisionLogo } from "@/components/ui/MedVisionLogo";
import { ThemeSwitcher } from "@/components/ui/ThemeSwitcher";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";
import { ImageUploader } from "@/components/segmentation/ImageUploader";
import { AnalysisParametersPanel } from "@/components/segmentation/AnalysisParametersPanel";
import { AnalysisVisualization } from "@/components/segmentation/AnalysisVisualization";
import { GroundTruthEvaluation } from "@/components/segmentation/GroundTruthEvaluation";
import { authApi, segmentationApi, ApiRequestError } from "@/lib/api";
import type { AnalysisOptions, MedicalImage, AnalysisParameters, AnalysisResult } from "@/lib/segmentation-types";

export default function SegmentPage() {
  const router = useRouter();

  const [options, setOptions] = useState<AnalysisOptions | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryKey, setRetryKey] = useState(0);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadedImage, setUploadedImage] = useState<MedicalImage | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");

  const [parameters, setParameters] = useState<AnalysisParameters>({
    name: "",
    algorithm: "OTSU",
    blurKernelSize: 3,
    invert: false,
  });
  const [segmenting, setSegmenting] = useState(false);
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);
  const [analysisError, setAnalysisError] = useState("");

  async function handleSegment() {
    if (!uploadedImage || segmenting || uploading) return;

    const name = parameters.name?.trim() ?? "";

    if (!name || name.length > 120) {
      setAnalysisError("Tahlil nomini kiriting (maksimal 120 ta belgi).");
      return;
    }

    setSegmenting(true);
    setAnalysisError("");
    setAnalysis(null);

    try {
      const request: AnalysisParameters = {
        name,
        algorithm: parameters.algorithm,
        blurKernelSize: parameters.blurKernelSize,
        invert: parameters.invert,
        ...(parameters.algorithm === "MANUAL"
          ? { threshold: parameters.threshold ?? 127 }
          : {}),
        ...(parameters.algorithm === "ADAPTIVE"
          ? {
              blockSize: parameters.blockSize ?? 11,
              c: parameters.c ?? 2,
            }
          : {}),
      };

      await segmentationApi.validate(request);
      const result = await segmentationApi.execute(uploadedImage.id, request);
      setAnalysis(result);
    } catch (err: unknown) {
      if (
        err instanceof ApiRequestError &&
        (err.status === 401 || err.status === 403)
      ) {
        router.replace("/login?next=%2Fdashboard%2Fsegment");
        return;
      }

      setAnalysisError(
        err instanceof Error
          ? err.message
          : "Segmentatsiyani bajarishda xatolik yuz berdi.",
      );
    } finally {
      setSegmenting(false);
    }
  }
  async function handleUpload() {
    if (!selectedFile || uploading || segmenting) return;

    setUploading(true);
    setUploadError("");
    setUploadedImage(null);

    try {
      const result = await segmentationApi.upload(selectedFile);
      setUploadedImage(result);
    } catch (err: unknown) {
      if (
        err instanceof ApiRequestError &&
        (err.status === 401 || err.status === 403)
      ) {
        router.replace("/login?next=%2Fdashboard%2Fsegment");
        return;
      }

      setUploadError(
        err instanceof Error
          ? err.message
          : "Rasmni serverga yuklashda xatolik yuz berdi.",
      );
    } finally {
      setUploading(false);
    }
  }

  useEffect(() => {
    let active = true;

    Promise.all([authApi.me(), segmentationApi.options()])
      .then(([, result]) => {
        if (active) setOptions(result);
      })
      .catch((err: unknown) => {
        if (!active) return;

        if (
          err instanceof ApiRequestError &&
          (err.status === 401 || err.status === 403)
        ) {
          router.replace("/login?next=%2Fdashboard%2Fsegment");
          return;
        }

        setError(
          err instanceof Error
            ? err.message
            : "Server bilan bog'lanib bo'lmadi.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [router, retryKey]);

  if (loading) {
    return (
      <main className="min-h-screen bg-[var(--background)]">
        <LoadingState
          fullPage
          title="MedVision Studio yuklanmoqda"
          description="Tahlil sozlamalari serverdan olinmoqda."
        />
      </main>
    );
  }

  if (!options) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[var(--background)] px-4">
        <div className="w-full max-w-lg">
          <ErrorState
            title="Tahlil sahifasi ochilmadi"
            description={error || "Server ma'lumotlari mavjud emas."}
            onRetry={() => {
              setLoading(true);
              setError("");
              setRetryKey((value) => value + 1);
            }}
          />
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-7xl px-5 py-7 sm:px-8">

        

        <div className="mb-8 flex flex-wrap items-start justify-between gap-5">
          <div>
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[var(--border)] bg-[var(--surface)] px-3 py-1.5 text-xs font-bold text-[var(--primary)]">
              <ScanLine size={14} />
              MEDVISION ANALYSIS STUDIO
            </div>

            <h1 className="text-3xl font-extrabold tracking-tight text-[var(--foreground)] sm:text-4xl">
              Segmentation Studio
            </h1>

            <p className="mt-3 max-w-2xl text-sm leading-7 text-[var(--muted)] sm:text-base">
              Tibbiy tasvirni yuklang, parametrlarni tanlang va segmentatsiya
              natijasini vizual tahlil qiling.
            </p>
          </div>

          <div className="flex items-center gap-2 rounded-2xl border border-[var(--border)] bg-[var(--surface)] px-4 py-3 text-sm text-[var(--muted)]">
            <ShieldCheck size={19} className="text-[var(--primary)]" />
            Himoyalangan sessiya
          </div>
        </div>

        <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">
          <section className="rounded-3xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm sm:p-8">
            <div className="mb-6">
              <p className="text-xs font-bold uppercase tracking-widest text-[var(--primary)]">
                01 / IMAGE INPUT
              </p>
              <h2 className="mt-2 text-xl font-bold text-[var(--foreground)]">
                Rasm yuklash
              </h2>
              <p className="mt-2 text-sm text-[var(--muted)]">
                Kompyuteringizdan PNG yoki JPEG tasvir tanlang.
              </p>
            </div>

            <ImageUploader
              options={options}
              disabled={uploading || segmenting}
              onChange={(image) => {
                setSelectedFile(image?.file ?? null);
                setUploadedImage(null);
                setUploadError("");
                setAnalysis(null);
                setAnalysisError("");
              }}
            />

            {selectedFile && (
              <div className="mt-6">
                <button
                  type="button"
                  onClick={handleUpload}
                  disabled={uploading || Boolean(uploadedImage)}
                  className="med-button med-button-primary w-full disabled:opacity-50"
                >
                  {uploading
                    ? "Serverga yuklanmoqda..."
                    : uploadedImage
                      ? "Rasm muvaffaqiyatli yuklandi"
                      : "Rasmni serverga yuklash"}
                </button>
              </div>
            )}

            {uploadError && (
              <div
                role="alert"
                className="mt-5 rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-700"
              >
                {uploadError}
              </div>
            )}

            {uploadedImage && (
              <div
                role="status"
                className="mt-5 rounded-2xl border border-[var(--border)] bg-[var(--surface-muted)] p-5"
              >
                <p className="font-bold text-[var(--primary)]">
                  Rasm serverga yuklandi!
                </p>
                <p className="mt-2 break-all text-sm text-[var(--muted)]">
                  Image ID: {uploadedImage.id}
                </p>
                <p className="mt-1 text-sm text-[var(--muted)]">
                  {uploadedImage.width} Ãƒâ€” {uploadedImage.height} px
                </p>
              </div>
            )}
          </section>

          <div className="space-y-6">
            <AnalysisParametersPanel
              value={parameters}
              onChange={(next) => {
                setParameters(next);
                setAnalysis(null);
                setAnalysisError("");
              }}
              disabled={segmenting}
            />

            <button
              type="button"
              onClick={handleSegment}
              disabled={!uploadedImage || segmenting || uploading || !parameters.name?.trim()}
              className="med-button med-button-primary w-full disabled:opacity-50"
            >
              {segmenting
                ? "Segmentatsiya bajarilmoqda..."
                : "Segmentatsiyani boshlash"}
            </button>

            {!uploadedImage && (
              <p className="text-center text-sm text-[var(--muted)]">
                Avval rasmni serverga yuklang.
              </p>
            )}

            {analysisError && (
              <div role="alert" className="rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-700">
                {analysisError}
              </div>
            )}

            {analysis && (
              <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
                <h3 className="text-lg font-bold text-[var(--foreground)]">
                  {analysis.name || "Segmentatsiya natijasi"}
                </h3>
                <p className="mt-3 text-sm text-[var(--muted)]">
                  Status: {analysis.status}
                </p>
                <p className="mt-2 break-all text-xs text-[var(--muted)]">
                  Analysis ID: {analysis.id}
                </p>
                {analysis.foregroundRatio !== null && (
                  <p className="mt-3 font-semibold text-[var(--primary)]">
                    Foreground: {(analysis.foregroundRatio * 100).toFixed(2)}%
                  </p>
                )}
                {analysis.processingTimeMs !== null && (
                  <p className="mt-2 text-sm text-[var(--muted)]">
                    Processing time: {analysis.processingTimeMs} ms
                  </p>
                )}
              </div>
            )}
          </div>
          {analysis?.status === "COMPLETED" && (
            <div className="space-y-6 xl:col-span-2">
              <AnalysisVisualization
                key={analysis.id}
                imageId={analysis.imageId}
                analysisId={analysis.id}
              />

              <GroundTruthEvaluation
                key={`evaluation-${analysis.id}`}
                analysis={analysis}
                onEvaluated={setAnalysis}
              />
            </div>
          )}
          <aside className="space-y-5">
            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-6 shadow-sm">
              <p className="text-xs font-bold uppercase tracking-widest text-[var(--primary)]">
                WORKSPACE
              </p>
              <h2 className="mt-3 text-lg font-bold text-[var(--foreground)]">
                Tahlil holati
              </h2>

              <div className="mt-5 space-y-4 text-sm">
                <div className="flex justify-between gap-3">
                  <span className="text-[var(--muted)]">Rasm</span>
                  <span className="max-w-36 truncate text-right font-semibold text-[var(--foreground)]">
                    {selectedFile?.name || "Tanlanmagan"}
                  </span>
                </div>
                <div className="flex justify-between gap-3">
                  <span className="text-[var(--muted)]">Tahlil nomi</span>
                  <span className="max-w-36 truncate text-right font-semibold text-[var(--foreground)]">
                    {parameters.name?.trim() || "Kiritilmagan"}
                  </span>
                </div>
              </div>
            </div>

            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface-muted)] p-6">
              <h3 className="font-bold text-[var(--foreground)]">
                Keyingi bosqich
              </h3>
              <p className="mt-2 text-sm leading-6 text-[var(--muted)]">
                Rasmni yuklang, tahlil nomini kiriting va segmentatsiyani boshlang.
              </p>
            </div>
          </aside>
        </div>
    </main>
  );
}

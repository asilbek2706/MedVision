"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AnalysisRenameButton } from "@/components/dashboard/AnalysisRenameButton";
import { useParams, useRouter } from "next/navigation";
import {  Clock3, ScanLine } from "lucide-react";


import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";
import { AnalysisVisualization } from "@/components/segmentation/AnalysisVisualization";
import { GroundTruthEvaluation } from "@/components/segmentation/GroundTruthEvaluation";
import type { AnalysisResult } from "@/lib/segmentation-types";

import {
  authApi,
  dashboardApi,
  ApiRequestError,
  type AnalysisHistoryItem,
  type MedicalImageItem,
} from "@/lib/api";

const dateText = (value: string) =>
  new Date(value).toLocaleString("uz-UZ");

export default function AnalysisDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = params.id;

  const [analysis, setAnalysis] = useState<AnalysisHistoryItem | null>(null);
  const [image, setImage] = useState<MedicalImageItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let active = true;

    Promise.all([authApi.me(), dashboardApi.analysis(id)])
      .then(async ([, result]) => {
        const imageResult = await dashboardApi.image(result.imageId);
        if (!active) return;
        setAnalysis(result);
        setImage(imageResult);
      })
      .catch((err: unknown) => {
        if (!active) return;

        if (
          err instanceof ApiRequestError &&
          (err.status === 401 || err.status === 403)
        ) {
          router.replace(
            `/login?next=${encodeURIComponent(`/dashboard/history/${id}`)}`,
          );
          return;
        }

        setError(
          err instanceof Error
            ? err.message
            : "Tahlil tafsilotlarini olishda xatolik.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [id, router, retryKey]);

  if (loading) {
    return (
      <LoadingState
        fullPage
        title="Tahlil yuklanmoqda"
        description="Natijalar serverdan olinmoqda."
      />
    );
  }

  if (!analysis || !image) {
    return (
      <main className="min-h-screen bg-[(--background)] px-4 py-10">
        <div className="mx-auto max-w-xl">
          <ErrorState
            title="Tahlil topilmadi"
            description={error || "Tahlil ma'lumotlari mavjud emas."}
            onRetry={() => {
              setLoading(true);
              setError("");
              setRetryKey((v) => v + 1);
            }}
          />
          <Link
            href="/dashboard/history"
            className="mt-5 inline-block text-sm font-semibold text-[(--primary)]"
          >
            Tarixga qaytish
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[var(--background)] px-4 py-8 sm:px-8">
      <div className="mx-auto max-w-6xl">


        <section className="med-card p-6 sm:p-8">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-bold uppercase tracking-widest text-[var(--primary)]">
                ANALYSIS DETAILS
              </p>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <h1 className="text-3xl font-extrabold text-[var(--foreground)]">
                  {analysis.name || "Nomsiz tahlil"}
                </h1>
                <AnalysisRenameButton
                  id={analysis.id}
                  currentName={analysis.name}
                  onRenamed={(name) =>
                    setAnalysis((previous) =>
                      previous ? { ...previous, name } : previous
                    )
                  }
                />
              </div>
              <p className="mt-2 text-sm text-[var(--muted)]">
                Rasm: {image.originalName} Â· {analysis.status}
              </p>
            </div>
            <ScanLine size={28} className="text-[var(--primary)]" />
          </div>

          <div className="mt-7 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-xl bg-[var(--surface-muted)] p-4">
              <p className="text-xs text-[var(--muted)]">Status</p>
              <p className="mt-2 font-bold text-[var(--foreground)]">
                {analysis.status}
              </p>
            </div>

            <div className="rounded-xl bg-[var(--surface-muted)] p-4">
              <p className="text-xs text-[var(--muted)]">Foreground</p>
              <p className="mt-2 font-bold text-[var(--foreground)]">
                {analysis.foregroundRatio === null
                  ? "â€”"
                  : `${(analysis.foregroundRatio * 100).toFixed(2)}%`}
              </p>
            </div>

            <div className="rounded-xl bg-[var(--surface-muted)] p-4">
              <p className="text-xs text-[var(--muted)]">Processing time</p>
              <p className="mt-2 font-bold text-[var(--foreground)]">
                {analysis.processingTimeMs === null
                  ? "â€”"
                  : `${analysis.processingTimeMs} ms`}
              </p>
            </div>

            <div className="rounded-xl bg-[var(--surface-muted)] p-4">
              <p className="flex items-center gap-2 text-xs text-[var(--muted)]">
                <Clock3 size={14} /> Yaratilgan
              </p>
              <p className="mt-2 text-sm font-bold text-[var(--foreground)]">
                {dateText(analysis.createdAt)}
              </p>
            </div>
          </div>

          <div className="mt-6 rounded-xl border border-[var(--border)] p-5">
            <h2 className="font-bold text-[var(--foreground)]">
              Rasm ma&apos;lumotlari
            </h2>
            <p className="mt-3 text-sm text-[var(--muted)]">
              O&apos;lcham: {image.width} {"\u00D7"} {image.height} px
            </p>
            <p className="mt-2 text-sm text-[var(--muted)]">
              Hajm: {(image.sizeBytes / 1024 / 1024).toFixed(2)} MB
            </p>
          </div>

          {analysis.status === "FAILED" && (
            <p role="alert" className="mt-6 rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-700">
              Tahlil bajarilmadi. Xatolik kodi: {analysis.errorCode || "Noma'lum"}
            </p>
          )}
        </section>

        {analysis.status === "COMPLETED" && (
          <div className="mt-6 space-y-6">
            <AnalysisVisualization
              imageId={analysis.imageId}
              analysisId={analysis.id}
            />

            <GroundTruthEvaluation
              key={`evaluation-${analysis.id}`}
              analysis={analysis}
              onEvaluated={(result: AnalysisResult) => {
                setAnalysis((previous) =>
                  previous
                    ? {
                        ...previous,
                        diceScore: result.diceScore,
                        iouScore: result.iouScore,
                        hasGroundTruth: result.hasGroundTruth,
                      }
                    : previous,
                );
              }}
            />
          </div>
        )}
      </div>
    </main>
  );
}

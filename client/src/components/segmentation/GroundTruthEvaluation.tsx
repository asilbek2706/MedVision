"use client";

import { useState } from "react";
import { CheckCircle2, FileImage, LoaderCircle } from "lucide-react";

import { ApiRequestError, segmentationApi } from "@/lib/api";
import type { AnalysisResult } from "@/lib/segmentation-types";

type Props = {
  analysis: AnalysisResult;
  onEvaluated: (result: AnalysisResult) => void;
};

const MAX_GROUND_TRUTH_SIZE = 10 * 1024 * 1024;

export function GroundTruthEvaluation({ analysis, onEvaluated }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");

  const evaluated = analysis.hasGroundTruth;

  async function handleEvaluate() {
    if (!file || uploading || evaluated || analysis.status !== "COMPLETED") {
      return;
    }

    setUploading(true);
    setError("");

    try {
      const result = await segmentationApi.evaluate(analysis.id, file);
      onEvaluated(result);
      setFile(null);
    } catch (err: unknown) {
      if (err instanceof ApiRequestError) {
        setError(err.message);
      } else {
        setError(
          err instanceof Error
            ? err.message
            : "Ground Truth baholashda xatolik yuz berdi.",
        );
      }
    } finally {
      setUploading(false);
    }
  }

  if (analysis.status !== "COMPLETED") {
    return null;
  }

  return (
    <section className="med-card p-5 sm:p-8">
      <div className="mb-5">
        <p className="text-xs font-bold uppercase tracking-widest text-[var(--primary)]">
          03 / SEGMENTATION EVALUATION
        </p>
        <h2 className="mt-2 text-xl font-bold text-[var(--foreground)]">
          Ground Truth baholash
        </h2>
        <p className="mt-2 text-sm leading-6 text-[var(--muted)]">
          Ekspert tomonidan tayyorlangan haqiqiy binar maskani PNG
          formatida yuklang. Dice va IoU faqat Ground Truth mavjud
          bo&apos;lganda hisoblanadi.
        </p>
      </div>

      {evaluated ? (
        <div className="space-y-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-[var(--primary)]">
            <CheckCircle2 size={19} />
            Ground Truth baholash yakunlangan
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface-muted)] p-5">
              <p className="text-xs font-semibold text-[var(--muted)]">
                Dice Score
              </p>
              <p className="mt-2 text-2xl font-bold text-[var(--foreground)]">
                {analysis.diceScore === null
                  ? "—"
                  : `${(analysis.diceScore * 100).toFixed(2)}%`}
              </p>
            </div>

            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface-muted)] p-5">
              <p className="text-xs font-semibold text-[var(--muted)]">
                IoU Score
              </p>
              <p className="mt-2 text-2xl font-bold text-[var(--foreground)]">
                {analysis.iouScore === null
                  ? "—"
                  : `${(analysis.iouScore * 100).toFixed(2)}%`}
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <label
            htmlFor="ground-truth-file"
            className="flex cursor-pointer items-center gap-3 rounded-2xl border border-[var(--border)] bg-[var(--surface-muted)] p-4"
          >
            <FileImage size={23} className="shrink-0 text-[var(--primary)]" />

            <span className="min-w-0 flex-1">
              <span className="block text-sm font-semibold text-[var(--foreground)]">
                {file?.name ?? "Ground Truth PNG tanlang"}
              </span>
              <span className="mt-1 block text-xs text-[var(--muted)]">
                PNG format, maksimal 10 MB
              </span>
            </span>
          </label>

          <input
            id="ground-truth-file"
            type="file"
            accept=".png,image/png"
            disabled={uploading}
            className="block w-full text-sm text-[var(--muted)] file:mr-3 file:rounded-lg file:border-0 file:bg-[var(--surface-muted)] file:px-3 file:py-2 file:font-semibold"
            onChange={(event) => {
              const selected = event.target.files?.[0] ?? null;
              setError("");
              setFile(null);

              if (!selected) return;

              if (
                selected.type !== "image/png" ||
                !selected.name.toLowerCase().endsWith(".png")
              ) {
                setError("Faqat PNG formatidagi Ground Truth qabul qilinadi.");
                event.target.value = "";
                return;
              }

              if (selected.size === 0 || selected.size > MAX_GROUND_TRUTH_SIZE) {
                setError("Ground Truth PNG hajmi 1 baytdan 10 MB gacha bo'lishi kerak.");
                event.target.value = "";
                return;
              }

              setFile(selected);
            }}
          />

          <button
            type="button"
            onClick={handleEvaluate}
            disabled={!file || uploading}
            className="med-button med-button-primary flex w-full items-center justify-center gap-2 disabled:opacity-50"
          >
            {uploading && <LoaderCircle size={18} className="animate-spin" />}
            {uploading ? "Baholanmoqda..." : "Dice va IoU ni hisoblash"}
          </button>

          {error && (
            <p
              role="alert"
              className="rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-700"
            >
              {error}
            </p>
          )}
        </div>
      )}
    </section>
  );
}
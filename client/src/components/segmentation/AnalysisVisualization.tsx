"use client";

import { useEffect, useState } from "react";
import { Images, AlertCircle, LoaderCircle, Download } from "lucide-react";
import { segmentationApi } from "@/lib/api";

type Props = {
  imageId: string;
  analysisId: string;
};

type ImageUrls = {
  original: string;
  mask: string;
  overlay: string;
};

export function AnalysisVisualization({ imageId, analysisId }: Props) {
  const [urls, setUrls] = useState<ImageUrls | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [opacity, setOpacity] = useState(65);
  const [downloading, setDownloading] = useState<"mask" | "overlay" | null>(null);
  const [downloadError, setDownloadError] = useState("");

  async function handleDownload(type: "mask" | "overlay") {
    if (downloading) return;

    setDownloading(type);
    setDownloadError("");

    try {
      const blob =
        type === "mask"
          ? await segmentationApi.downloadMask(analysisId)
          : await segmentationApi.downloadOverlay(analysisId);

      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");

      link.href = url;
      link.download = `medvision-${type}-${analysisId}.png`;

      document.body.appendChild(link);
      link.click();
      link.remove();

      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (err: unknown) {
      setDownloadError(
        err instanceof Error
          ? err.message
          : "Natijani yuklab olishda xatolik yuz berdi.",
      );
    } finally {
      setDownloading(null);
    }
  }

  useEffect(() => {
    let active = true;
    const createdUrls: string[] = [];

    Promise.all([
      segmentationApi.original(imageId),
      segmentationApi.mask(analysisId),
      segmentationApi.overlay(analysisId),
    ])
      .then(([original, mask, overlay]) => {
        if (!active) return;

        const result = {
          original: URL.createObjectURL(original),
          mask: URL.createObjectURL(mask),
          overlay: URL.createObjectURL(overlay),
        };

        createdUrls.push(result.original, result.mask, result.overlay);
        setUrls(result);
      })
      .catch((err: unknown) => {
        if (!active) return;
        setError(
          err instanceof Error
            ? err.message
            : "Tahlil rasmlarini yuklab bo'lmadi.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      createdUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [imageId, analysisId]);

  return (
    <section className="med-card p-5 sm:p-8">
      <div className="mb-7 flex items-center gap-3">
        <div className="flex size-11 items-center justify-center rounded-xl bg-[var(--primary-light)] text-[var(--primary)]">
          <Images size={21} />
        </div>
        <div>
          <p className="text-xs font-bold uppercase tracking-widest text-[var(--primary)]">
            03 / VISUALIZATION
          </p>
          <h2 className="mt-1 text-xl font-bold text-[var(--foreground)]">
            Segmentatsiya natijalari
          </h2>
        </div>
      </div>

      {loading && (
        <div role="status" className="flex items-center justify-center gap-3 py-14 text-[var(--muted)]">
          <LoaderCircle size={20} className="animate-spin" />
          Rasmlar yuklanmoqda...
        </div>
      )}

      {error && (
        <div role="alert" className="flex items-center gap-3 rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-700">
          <AlertCircle size={20} />
          {error}
        </div>
      )}

      {urls && !loading && (
        <div className="grid items-stretch gap-5 md:grid-cols-3">
          <ImageCard
            title="Original"
            description="Yuklangan tibbiy tasvir"
            src={urls.original}
          />

          <ImageCard
            title="Mask"
            description="OTSU segmentatsiya maskasi"
            src={urls.mask}
            onDownload={() => void handleDownload("mask")}
            downloading={downloading === "mask"}
          />

          <ImageCard
            title="Overlay"
            description="Segmentatsiya shaffofligi"
            src={urls.original}
            overlaySrc={urls.overlay}
            opacity={opacity}
            onOpacityChange={setOpacity}
            onDownload={() => void handleDownload("overlay")}
            downloading={downloading === "overlay"}
          />
        </div>
      )}

      {downloadError && (
        <div
          role="alert"
          className="mt-5 rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300"
        >
          {downloadError}
        </div>
      )}
    </section>
  );
}

function ImageCard({
  title,
  description,
  src,
  overlaySrc,
  opacity,
  onOpacityChange,
  onDownload,
  downloading = false,
}: {
  title: string;
  description: string;
  src: string;
  overlaySrc?: string;
  opacity?: number;
  onOpacityChange?: (value: number) => void;
  onDownload?: () => void;
  downloading?: boolean;
}) {
  return (
    <div className="flex min-w-0 flex-col overflow-hidden rounded-2xl border border-[var(--border)] bg-[var(--surface)]">
      <div className="flex aspect-square items-center justify-center bg-[var(--surface-muted)] p-3">
        <div className="relative flex h-full w-full items-center justify-center overflow-hidden rounded-lg">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={src}
            alt={title}
            className="absolute inset-0 h-full w-full object-contain"
          />

          {overlaySrc && (
            /* eslint-disable-next-line @next/next/no-img-element */
            <img
              src={overlaySrc}
              alt="Segmentatsiya overlay"
              style={{ opacity: (opacity ?? 65) / 100 }}
              className="absolute inset-0 h-full w-full object-contain"
            />
          )}
        </div>
      </div>

      <div className="flex flex-1 flex-col border-t border-[var(--border)] p-4">
        <div className="flex items-center justify-between gap-2">
          <h3 className="font-bold text-[var(--foreground)]">
            {title}
          </h3>

          {overlaySrc && (
            <span className="text-sm font-bold text-[var(--primary)]">
              {opacity}%
            </span>
          )}
        </div>

        <p className="mt-1 text-xs text-[var(--muted)]">
          {description}
        </p>

        {onDownload && (
          <button
            type="button"
            onClick={onDownload}
            disabled={downloading}
            className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl border border-[var(--border)] bg-[var(--surface-muted)] px-4 py-3 text-sm font-semibold text-[var(--foreground)] transition-colors hover:border-[var(--primary)] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {downloading ? (
              <LoaderCircle size={16} className="animate-spin" />
            ) : (
              <Download size={16} />
            )}
            {downloading ? "Yuklanmoqda..." : "Download PNG"}
          </button>
        )}

        {overlaySrc && onOpacityChange && (
          <div className="mt-auto pt-4">
            <input
              type="range"
              min="0"
              max="100"
              step="5"
              value={opacity ?? 65}
              onChange={(event) =>
                onOpacityChange(Number(event.target.value))
              }
              aria-label="Overlay opacity"
              className="w-full accent-[var(--primary)]"
            />
            <div className="mt-1 flex justify-between text-xs text-[var(--muted)]">
              <span>0%</span>
              <span>100%</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

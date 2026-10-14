"use client";

import { SlidersHorizontal, Info } from "lucide-react";
import type { AnalysisParameters } from "@/lib/segmentation-types";

type Props = {
  value: AnalysisParameters;
  onChange: (value: AnalysisParameters) => void;
  disabled?: boolean;
};

const KERNEL_SIZES = [1, 3, 5, 7, 9, 11, 13, 15];

export function AnalysisParametersPanel({
  value,
  onChange,
  disabled = false,
}: Props) {
  return (
    <section className="med-card p-5 sm:p-8">
      <div className="mb-6 flex items-center gap-3">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-[var(--primary-light)] text-[var(--primary)]">
          <SlidersHorizontal size={21} />
        </div>

        <div>
          <p className="text-xs font-bold uppercase tracking-widest text-[var(--primary)]">
            02 / ANALYSIS SETTINGS
          </p>
          <h2 className="mt-1 text-xl font-bold text-[var(--foreground)]">
            Segmentatsiya parametrlari
          </h2>
        </div>
      </div>

      <div className="space-y-6">
        <div>
          <label
            htmlFor="analysis-name"
            className="mb-2 block text-sm font-semibold text-[var(--foreground)]"
          >
            Tahlil nomi <span className="text-red-500">*</span>
          </label>
          <input
            id="analysis-name"
            type="text"
            value={value.name ?? ""}
            maxLength={120}
            required
            disabled={disabled}
            placeholder="Masalan: Miya MRT - 1-tahlil"
            onChange={(event) =>
              onChange({ ...value, name: event.target.value })
            }
            className="med-input w-full disabled:opacity-50"
          />
          <p className="mt-2 text-xs text-[var(--muted)]">
            Tahlilni Dashboard va History sahifalarida shu nom bilan topasiz.
          </p>
        </div>

        <div>
          <label
            htmlFor="segmentation-algorithm"
            className="mb-2 block text-sm font-semibold text-[var(--foreground)]"
          >
            Segmentatsiya algoritmi
          </label>

          <select
            id="segmentation-algorithm"
            value={value.algorithm}
            disabled={disabled}
            onChange={(event) => {
              const algorithm = event.target.value as AnalysisParameters["algorithm"];

              onChange({
                ...value,
                algorithm,
                threshold: algorithm === "MANUAL" ? (value.threshold ?? 127) : undefined,
                blockSize: algorithm === "ADAPTIVE" ? (value.blockSize ?? 11) : undefined,
                c: algorithm === "ADAPTIVE" ? (value.c ?? 2) : undefined,
              });
            }}
            className="med-input w-full disabled:opacity-50"
          >
            <option value="OTSU">OTSU - avtomatik threshold</option>
            <option value="MANUAL">MANUAL - qo&apos;lda threshold</option>
            <option value="ADAPTIVE">ADAPTIVE - lokal threshold</option>
          </select>

          <p className="mt-2 text-xs leading-5 text-[var(--muted)]">
            Algoritmni tanlang va kerakli parametrlarni sozlang.
          </p>
        </div>

        {value.algorithm === "MANUAL" && (
          <div>
            <label
              htmlFor="manual-threshold"
              className="mb-2 block text-sm font-semibold text-[var(--foreground)]"
            >
              Threshold: {value.threshold ?? 127}
            </label>

            <input
              id="manual-threshold"
              type="range"
              min={0}
              max={255}
              step={1}
              value={value.threshold ?? 127}
              disabled={disabled}
              onChange={(event) =>
                onChange({
                  ...value,
                  threshold: Number(event.target.value),
                })
              }
              className="w-full accent-[var(--primary)] disabled:opacity-50"
            />

            <p className="mt-2 text-xs text-[var(--muted)]">
              0 dan 255 gacha bo&apos;lgan threshold qiymati.
            </p>
          </div>
        )}

        {value.algorithm === "ADAPTIVE" && (
          <div className="space-y-5">
            <div>
              <label
                htmlFor="adaptive-block-size"
                className="mb-2 block text-sm font-semibold text-[var(--foreground)]"
              >
                Block Size
              </label>

              <select
                id="adaptive-block-size"
                value={value.blockSize ?? 11}
                disabled={disabled}
                onChange={(event) =>
                  onChange({
                    ...value,
                    blockSize: Number(event.target.value),
                  })
                }
                className="med-input w-full disabled:opacity-50"
              >
                {Array.from({ length: 15 }, (_, index) => index * 2 + 3).map(
                  (size) => (
                    <option key={size} value={size}>
                      {size}
                    </option>
                  ),
                )}
              </select>

              <p className="mt-2 text-xs text-[var(--muted)]">
                Lokal threshold uchun toq o&apos;lcham (3-31).
              </p>
            </div>

            <div>
              <label
                htmlFor="adaptive-c"
                className="mb-2 block text-sm font-semibold text-[var(--foreground)]"
              >
                C: {value.c ?? 2}
              </label>

              <input
                id="adaptive-c"
                type="range"
                min={-50}
                max={50}
                step={1}
                value={value.c ?? 2}
                disabled={disabled}
                onChange={(event) =>
                  onChange({
                    ...value,
                    c: Number(event.target.value),
                  })
                }
                className="w-full accent-[var(--primary)] disabled:opacity-50"
              />

              <p className="mt-2 text-xs text-[var(--muted)]">
                Lokal threshold hisobidagi C konstantasi.
              </p>
            </div>
          </div>
        )}
        <div>
          <label
            htmlFor="blur-kernel"
            className="mb-2 block text-sm font-semibold text-[var(--foreground)]"
          >
            Blur Kernel Size
          </label>

          <select
            id="blur-kernel"
            value={value.blurKernelSize}
            disabled={disabled}
            onChange={(event) =>
              onChange({
                ...value,
                blurKernelSize: Number(event.target.value),
              })
            }
            className="med-input w-full disabled:opacity-50"
          >
            {KERNEL_SIZES.map((size) => (
              <option key={size} value={size}>
                {size} {"\u00D7"} {size}
                {size === 1 ? " (Blur off)" : ""}
              </option>
            ))}
          </select>

          <div className="mt-3 flex items-start gap-2 text-xs leading-5 text-[var(--muted)]">
            <Info size={15} className="mt-0.5 shrink-0" />
            Kattaroq kernel tasvirdagi mayda shovqinlarni kamaytiradi.
          </div>
        </div>

        <label className="flex cursor-pointer items-center justify-between gap-4 rounded-2xl border border-[var(--border)] bg-[var(--surface-muted)] p-4">
          <div>
            <p className="text-sm font-semibold text-[var(--foreground)]">
              Invert Mask
            </p>
            <p className="mt-1 text-xs leading-5 text-[var(--muted)]">
              Maskadagi oq va qora hududlarni almashtiradi.
            </p>
          </div>

          <input
            type="checkbox"
            checked={value.invert}
            disabled={disabled}
            onChange={(event) =>
              onChange({
                ...value,
                invert: event.target.checked,
              })
            }
            className="size-5 shrink-0 accent-[var(--primary)]"
          />
        </label>
      </div>
    </section>
  );
}

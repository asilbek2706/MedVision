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
          <p className="mb-2 text-sm font-semibold text-[var(--foreground)]">
            Algoritm
          </p>

          <div className="rounded-xl border border-[var(--border)] bg-[var(--surface-muted)] px-4 py-4">
            <p className="text-sm font-bold text-[var(--foreground)]">
              OTSU
            </p>
            <p className="mt-1 text-xs text-[var(--muted)]">
              Automatic Thresholding
            </p>
          </div>
        </div>

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
                {size} × {size}
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

"use client";

import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, DragEvent } from "react";
import { UploadCloud, FileImage, X, CheckCircle2 } from "lucide-react";
import type { AnalysisOptions } from "@/lib/segmentation-types";

type SelectedImage = {
  file: File;
  previewUrl: string;
  width: number;
  height: number;
};

type ImageUploaderProps = {
  options: AnalysisOptions;
  onChange: (image: SelectedImage | null) => void;
  disabled?: boolean;
};

export function ImageUploader({ options, onChange, disabled = false }: ImageUploaderProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const previewRef = useRef<string | null>(null);

  const [image, setImage] = useState<SelectedImage | null>(null);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    return () => {
      if (previewRef.current) {
        URL.revokeObjectURL(previewRef.current);
      }
    };
  }, []);

  function clearImage() {
    if (disabled) return;
    if (previewRef.current) {
      URL.revokeObjectURL(previewRef.current);
      previewRef.current = null;
    }

    setImage(null);
    setError("");
    onChange(null);

    if (inputRef.current) inputRef.current.value = "";
  }

  async function selectFile(file?: File) {
    if (!file || disabled) return;

    setError("");

    const acceptedTypes = ["image/png", "image/jpeg"];

    if (!acceptedTypes.includes(file.type)) {
      setError("Faqat PNG va JPEG formatidagi rasmlar qabul qilinadi.");
      return;
    }

    if (file.size > options.maxFileSizeBytes) {
      setError(
        `Fayl hajmi ${(options.maxFileSizeBytes / 1048576).toFixed(0)} MB dan oshmasligi kerak.`,
      );
      return;
    }

    const url = URL.createObjectURL(file);
    const preview = new Image();
    preview.src = url;

    try {
      await preview.decode();

      if (
        preview.naturalWidth > options.maxWidth ||
        preview.naturalHeight > options.maxHeight
      ) {
        URL.revokeObjectURL(url);
        setError(
          `Rasm o'lchami ${options.maxWidth} x ${options.maxHeight} px dan oshmasligi kerak.`,
        );
        return;
      }

      if (previewRef.current) {
        URL.revokeObjectURL(previewRef.current);
      }

      previewRef.current = url;

      const selected = {
        file,
        previewUrl: url,
        width: preview.naturalWidth,
        height: preview.naturalHeight,
      };

      setImage(selected);
      onChange(selected);
    } catch {
      URL.revokeObjectURL(url);
      setError("Rasmni o'qib bo'lmadi. Boshqa fayl tanlang.");
    }
  }

  function handleInput(event: ChangeEvent<HTMLInputElement>) {
    void selectFile(event.target.files?.[0]);
    event.target.value = "";
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragging(false);
    void selectFile(event.dataTransfer.files[0]);
  }

  return (
    <div className="space-y-5">
      <div
        onDragOver={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragLeave={(event) => {
          event.preventDefault();
          setDragging(false);
        }}
        onDrop={handleDrop}
        className={`relative rounded-3xl border-2 border-dashed p-8 text-center transition-all sm:p-12 ${
          dragging
            ? "border-[var(--primary)] bg-[var(--primary-light)]"
            : "border-[var(--border)] bg-[var(--surface-muted)]"
        }`}
      >
        <input
          ref={inputRef}
          type="file"
          disabled={disabled}
          accept="image/png,image/jpeg"
          className="sr-only"
          onChange={handleInput}
          aria-label="Tibbiy rasm tanlash"
        />

        <div className="mx-auto mb-5 flex size-16 items-center justify-center rounded-2xl bg-[var(--surface)] text-[var(--primary)] shadow-sm">
          <UploadCloud size={30} strokeWidth={1.8} />
        </div>

        <h3 className="text-lg font-bold text-[var(--foreground)]">
          Tibbiy tasvirni shu yerga tashlang
        </h3>

        <p className="mt-2 text-sm text-[var(--muted)]">
          PNG yoki JPEG · Maksimal {(options.maxFileSizeBytes / 1048576).toFixed(0)} MB
        </p>

        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={disabled}
          className="med-button med-button-primary mt-6"
        >
          <FileImage size={18} />
          Fayl tanlash
        </button>
      </div>

      {error && (
        <div role="alert" className="rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
          {error}
        </div>
      )}

      {image && (
        <div className="med-card overflow-hidden p-5 sm:p-6">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2 text-sm font-semibold text-[var(--primary)]">
              <CheckCircle2 size={18} />
              Rasm tayyor
            </div>

            <button
              type="button"
              onClick={clearImage}
              disabled={disabled}
              aria-label="Rasmni olib tashlash"
              className="rounded-lg p-2 text-[var(--muted)] hover:bg-[var(--surface-muted)]"
            >
              <X size={18} />
            </button>
          </div>

          <div className="flex min-h-56 items-center justify-center overflow-hidden rounded-2xl bg-[var(--surface-muted)] p-4">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={image.previewUrl}
              alt="Yuklashdan oldingi rasm ko'rinishi"
              className="max-h-80 max-w-full rounded-lg object-contain"
            />
          </div>

          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <Metadata label="Fayl nomi" value={image.file.name} />
            <Metadata label="Format" value={image.file.type} />
            <Metadata label="Hajm" value={`${(image.file.size / 1048576).toFixed(2)} MB`} />
            <Metadata label="O'lcham" value={`${image.width} x ${image.height} px`} />
          </div>
        </div>
      )}
    </div>
  );
}

function Metadata({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <p className="text-xs font-medium text-[var(--muted)]">{label}</p>
      <p className="mt-1 break-words text-sm font-semibold text-[var(--foreground)]">
        {value}
      </p>
    </div>
  );
}

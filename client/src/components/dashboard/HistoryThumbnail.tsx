"use client";

import { useEffect, useState } from "react";
import { ImageIcon } from "lucide-react";
import { segmentationApi } from "@/lib/api";

export function HistoryThumbnail({ imageId }: { imageId: string }) {
  const [url, setUrl] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    let objectUrl: string | null = null;

    segmentationApi.original(imageId)
      .then((blob) => {
        if (!active) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => {
        if (active) setUrl(null);
      });

    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [imageId]);

  return (
    <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-[var(--border)] bg-[var(--surface)]">
      {url ? (
        // Authenticated image is loaded as an object URL.
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={url}
          alt="Yuklangan tibbiy rasm"
          className="h-full w-full object-cover"
        />
      ) : (
        <ImageIcon size={24} className="text-[var(--muted)]" />
      )}
    </div>
  );
}

"use client";

import { useState } from "react";
import { Pencil, Check, X } from "lucide-react";
import { dashboardApi } from "@/lib/api";

type Props = {
  id: string;
  currentName: string;
  onRenamed: (name: string) => void;
};

export function AnalysisRenameButton({
  id,
  currentName,
  onRenamed,
}: Props) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(currentName);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function save() {
    const trimmed = name.trim();

    if (!trimmed || trimmed.length > 120) {
      setError("Tahlil nomi 1-120 ta belgi bo'lishi kerak.");
      return;
    }

    if (trimmed === currentName) {
      setEditing(false);
      return;
    }

    setSaving(true);
    setError("");

    try {
      const result = await dashboardApi.renameAnalysis(id, trimmed);
      onRenamed(result.name);
      setEditing(false);
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "Tahlil nomini saqlab bo'lmadi."
      );
    } finally {
      setSaving(false);
    }
  }

  if (!editing) {
    return (
      <button
        type="button"
        onClick={(event) => {
          event.preventDefault();
          event.stopPropagation();
          setName(currentName);
          setError("");
          setEditing(true);
        }}
        aria-label="Tahlil nomini o'zgartirish"
        title="Nomini o'zgartirish"
        className="rounded-lg p-2 text-[var(--muted)] transition-colors hover:bg-[var(--surface-muted)] hover:text-[var(--primary)]"
      >
        <Pencil size={16} />
      </button>
    );
  }

  return (
    <div
      className="flex flex-wrap items-center gap-2"
      onClick={(event) => {
        event.preventDefault();
        event.stopPropagation();
      }}
    >
      <input
        autoFocus
        value={name}
        maxLength={120}
        disabled={saving}
        onChange={(event) => setName(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            event.preventDefault();
            void save();
          }
          if (event.key === "Escape") {
            setEditing(false);
            setError("");
          }
        }}
        className="med-input min-w-0 max-w-56"
      />

      <button
        type="button"
        disabled={saving}
        onClick={() => void save()}
        title="Saqlash"
        className="rounded-lg p-2 text-green-600 disabled:opacity-50"
      >
        <Check size={17} />
      </button>

      <button
        type="button"
        disabled={saving}
        onClick={() => {
          setEditing(false);
          setError("");
        }}
        title="Bekor qilish"
        className="rounded-lg p-2 text-red-500 disabled:opacity-50"
      >
        <X size={17} />
      </button>

      {error && (
        <p role="alert" className="w-full text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}
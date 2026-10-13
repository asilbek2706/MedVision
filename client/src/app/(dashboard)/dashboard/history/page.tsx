"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AnalysisRenameButton } from "@/components/dashboard/AnalysisRenameButton";
import { useRouter } from "next/navigation";
import { ArrowLeft, RefreshCw } from "lucide-react";
import { MedVisionLogo } from "@/components/ui/MedVisionLogo";
import { ThemeSwitcher } from "@/components/ui/ThemeSwitcher";
import { HistoryThumbnail } from "@/components/dashboard/HistoryThumbnail";
import {
  authApi,
  dashboardApi,
  ApiRequestError,
  type AnalysisStatus,
  type AnalysisHistoryItem,
  type PageResponse,
} from "@/lib/api";

const dateText = (value: string) =>
  new Date(value).toLocaleString("uz-UZ");

function dateStart(value: string): string | undefined {
  return value ? new Date(`${value}T00:00:00`).toISOString() : undefined;
}

function dateEnd(value: string): string | undefined {
  return value ? new Date(`${value}T23:59:59.999`).toISOString() : undefined;
}

export default function HistoryPage() {
  const router = useRouter();
  const [status, setStatus] = useState<AnalysisStatus | "">("");

  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);
  const [result, setResult] =
    useState<PageResponse<AnalysisHistoryItem> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    setLoading(true);
    setError("");

    Promise.all([
      authApi.me(),
      dashboardApi.analyses({
        page,
        size: 10,
        status: status || undefined,
        from: dateStart(from),
        to: dateEnd(to),
      }),
    ])
      .then(([, data]) => {
        if (active) setResult(data);
      })
      .catch((err: unknown) => {
        if (!active) return;
        if (
          err instanceof ApiRequestError &&
          (err.status === 401 || err.status === 403)
        ) {
          router.replace("/login?next=%2Fdashboard%2Fhistory");
          return;
        }
        setError(
          err instanceof Error
            ? err.message
            : "Tahlillar tarixini yuklashda xatolik.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [router, page, status, from, to, refreshKey]);

  const filterClass =
    "w-full rounded-xl border border-[var(--border)] bg-[var(--surface)] px-3 py-2.5 text-sm text-[var(--foreground)]";

  return (
    <main className="min-h-screen bg-[var(--background)] px-4 py-8 sm:px-8">
      <div className="mx-auto max-w-6xl">
        <header className="mb-8 flex items-center justify-between gap-4">
          <MedVisionLogo href="/dashboard" size="header" />
          <ThemeSwitcher />
        </header>

        <Link
          href="/dashboard"
          className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-[var(--muted)] hover:text-[var(--primary)]"
        >
          <ArrowLeft size={16} />
          Dashboardga qaytish
        </Link>

        <section className="med-card p-5 sm:p-8">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-xs font-bold uppercase tracking-widest text-[var(--primary)]">
                MEDVISION HISTORY
              </p>
              <h1 className="mt-2 text-3xl font-extrabold text-[var(--foreground)]">
                Tahlillar tarixi
              </h1>
              <p className="mt-2 text-sm text-[var(--muted)]">
                Oldingi segmentatsiya natijalarini ko'ring va filtrlang.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setRefreshKey((v) => v + 1)}
              disabled={loading}
              className="inline-flex items-center gap-2 rounded-xl border border-[var(--border)] px-4 py-2 text-sm font-semibold disabled:opacity-50"
            >
              <RefreshCw size={16} />
              Yangilash
            </button>
          </div>

          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <label className="space-y-2 text-sm text-[var(--muted)]">
              <span>Status</span>
              <select
                value={status}
                onChange={(e) => {
                  setStatus(e.target.value as AnalysisStatus | "");
                  setPage(0);
                }}
                className={filterClass}
              >
                <option value="">Barchasi</option>
                <option value="COMPLETED">Completed</option>
                <option value="FAILED">Failed</option>
                <option value="PROCESSING">Processing</option>
              </select>
            </label>

            <label className="space-y-2 text-sm text-[var(--muted)]">
              <span>Dan</span>
              <input
                type="date"
                value={from}
                max={to || undefined}
                onChange={(e) => {
                  setFrom(e.target.value);
                  setPage(0);
                }}
                className={filterClass}
              />
            </label>

            <label className="space-y-2 text-sm text-[var(--muted)]">
              <span>Gacha</span>
              <input
                type="date"
                value={to}
                min={from || undefined}
                onChange={(e) => {
                  setTo(e.target.value);
                  setPage(0);
                }}
                className={filterClass}
              />
            </label>
          </div>

          <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-[var(--muted)]">
              {result ? `Jami: ${result.totalElements} ta tahlil` : "Tahlillar"}
            </p>
            <button
              type="button"
              onClick={() => {
                setStatus("");
                            setFrom("");
                setTo("");
                setPage(0);
              }}
              className="text-sm font-semibold text-[var(--primary)] hover:underline"
            >
              Filterlarni tozalash
            </button>
          </div>

          {error && (
            <p role="alert" className="mt-5 rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-700">
              {error}
            </p>
          )}

          <div className="mt-5 space-y-3">
            {loading ? (
              <p className="py-8 text-center text-[var(--muted)]">
                Yuklanmoqda...
              </p>
            ) : result?.items.length === 0 ? (
              <p className="py-8 text-center text-[var(--muted)]">
                Bu filterlar bo'yicha tahlillar topilmadi.
              </p>
            ) : (
              result?.items.map((item) => (
                <div
                  key={item.id}
                  className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-[var(--border)] bg-[var(--surface-muted)] p-4"
                >
                  <Link
                    href={`/dashboard/history/${item.id}`}
                    className="flex min-w-0 flex-1 items-center gap-4 hover:opacity-80"
                  >
                    <HistoryThumbnail imageId={item.imageId} />
                    <div className="min-w-0">
                    <p className="font-bold text-[var(--foreground)]">
                      {item.name || "Nomsiz tahlil"}
                    </p>
                    <p className="mt-1 text-xs text-[var(--muted)]">
                      {dateText(item.createdAt)}
                    </p>
                    <p className="mt-1 text-xs text-[var(--muted)]">
                      Image ID: {item.imageId}
                    </p>
                    </div>
                  </Link>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-[var(--primary)]">
                      {item.status}
                    </span>
                    <AnalysisRenameButton
                      id={item.id}
                      currentName={item.name}
                      onRenamed={(name) =>
                        setResult((previous) =>
                          previous
                            ? {
                                ...previous,
                                items: previous.items.map((entry) =>
                                  entry.id === item.id
                                    ? { ...entry, name }
                                    : entry
                                ),
                              }
                            : previous
                        )
                      }
                    />
                  </div>
                </div>
              ))
            )}
          </div>

          {result && result.totalPages > 1 && (
            <div className="mt-7 flex items-center justify-between gap-4">
              <button
                type="button"
                disabled={loading || result.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="med-button disabled:opacity-40"
              >
                Oldingi
              </button>

              <span className="text-sm text-[var(--muted)]">
                {result.page + 1} / {result.totalPages}
              </span>

              <button
                type="button"
                disabled={loading || result.last}
                onClick={() => setPage((p) => p + 1)}
                className="med-button disabled:opacity-40"
              >
                Keyingi
              </button>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

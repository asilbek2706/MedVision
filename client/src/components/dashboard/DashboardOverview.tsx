"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { AnalysisRenameButton } from "@/components/dashboard/AnalysisRenameButton";
import { HistoryThumbnail } from "@/components/dashboard/HistoryThumbnail";
import { Activity, CheckCircle2, Images, RefreshCw, XCircle } from "lucide-react";
import {
  dashboardApi,
  type DashboardStats,
  type AnalysisHistoryItem,
} from "@/lib/api";

const dateText = (value: string) =>
  new Date(value).toLocaleString("uz-UZ");

export function DashboardOverview() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recent, setRecent] = useState<AnalysisHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  const refresh = useCallback(() => {
    setRefreshKey((value) => value + 1);
  }, []);

  useEffect(() => {
    let active = true;

    Promise.all([
      dashboardApi.stats(),
      dashboardApi.recentAnalyses(5),
    ])
      .then(([summary, analyses]) => {
        if (!active) return;
        setStats(summary);
        setRecent(analyses.items);
        setError("");
      })
      .catch((err: unknown) => {
        if (!active) return;
        setError(
          err instanceof Error
            ? err.message
            : "Dashboard ma'lumotlarini olishda xatolik.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [refreshKey]);

  const cards = [
    {
      label: "Jami rasmlar",
      value: stats?.totalImages ?? 0,
      icon: Images,
    },
    {
      label: "Muvaffaqiyatli tahlillar",
      value: stats?.completedAnalyses ?? 0,
      icon: CheckCircle2,
    },
    {
      label: "Xatolik bilan tugagan",
      value: stats?.failedAnalyses ?? 0,
      icon: XCircle,
    },
  ];

  return (
    <div className="mt-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-xs font-bold uppercase tracking-widest text-[var(--primary)]">
            LIVE OVERVIEW
          </p>
          <h2 className="mt-1 text-xl font-bold text-[var(--foreground)]">
            Tahlillar statistikasi
          </h2>
        </div>

        <button
          type="button"
          onClick={() => {
            setLoading(true);
            refresh();
          }}
          disabled={loading}
          className="inline-flex items-center gap-2 rounded-xl border border-[var(--border)] px-4 py-2 text-sm font-semibold text-[var(--foreground)] disabled:opacity-50"
        >
          <RefreshCw size={16} />
          Yangilash
        </button>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        {cards.map(({ label, value, icon: Icon }) => (
          <div
            key={label}
            className="group border border-[var(--border)] bg-[var(--surface)] shadow-sm relative overflow-hidden rounded-2xl p-6"
          >
            <div className="inline-flex rounded-xl bg-[var(--primary-light)] p-3 text-[var(--primary)]">
              <Icon size={22} />
            </div>
            <p className="mt-5 text-4xl font-extrabold tracking-tight text-[var(--foreground)]">
              {loading && !stats ? "..." : value}
            </p>
            <p className="mt-2 text-sm font-medium text-[var(--muted)]">{label}</p>
          </div>
        ))}
      </div>

      <section className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm p-5 sm:p-6">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Activity size={20} className="text-[var(--primary)]" />
            <h3 className="text-lg font-bold text-[var(--foreground)]">
              So&apos;nggi tahlillar
            </h3>
          </div>

          <Link
            href="/dashboard/history"
            className="text-sm font-semibold text-[var(--primary)] hover:underline"
          >
            Barcha tahlillar
          </Link>
        </div>

        {error && (
          <p role="alert" className="mb-4 text-sm text-red-600">
            {error}
          </p>
        )}

        {loading && !stats ? (
          <p className="text-sm text-[var(--muted)]">Yuklanmoqda...</p>
        ) : recent.length === 0 ? (
          <p className="text-sm text-[var(--muted)]">
            Hozircha tahlillar mavjud emas.
          </p>
        ) : (
          <div className="space-y-3">
            {recent.map((item) => (
              <div
                key={item.id}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] p-4"
              >
                <Link
                  href={`/dashboard/history/${item.id}`}
                  className="flex min-w-0 flex-1 items-center gap-4 hover:opacity-80"
                >
                  <HistoryThumbnail imageId={item.imageId} />
                  <div className="min-w-0">
                  <p className="font-semibold text-[var(--foreground)]">
                    {item.name || "Nomsiz tahlil"}
                  </p>
                  <p className="mt-1 text-xs text-[var(--muted)]">
                    {dateText(item.createdAt)}
                  </p>
                  </div>
                </Link>

                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-[var(--primary)]">
                    {item.status}
                  </span>
                  <AnalysisRenameButton
                    id={item.id}
                    currentName={item.name}
                    onRenamed={(name) =>
                      setRecent((items) =>
                        items.map((entry) =>
                          entry.id === item.id ? { ...entry, name } : entry
                        )
                      )
                    }
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

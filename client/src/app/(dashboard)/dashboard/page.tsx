"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ThemeSwitcher } from "@/components/ui/ThemeSwitcher";
import { DashboardOverview } from "@/components/dashboard/DashboardOverview";
import { MedVisionLogo } from "@/components/ui/MedVisionLogo";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";
import {
  authApi,
  ApiRequestError,
  type CurrentUser,
} from "@/lib/api";


export default function DashboardPage() {
  const router = useRouter();

  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let active = true;

    authApi
      .me()
      .then((currentUser) => {
        if (active) setUser(currentUser);
      })
      .catch((err: unknown) => {
        if (!active) return;

        if (
          err instanceof ApiRequestError &&
          (err.status === 401 || err.status === 403)
        ) {
          router.replace("/login?next=%2Fdashboard");
          return;
        }

        setError(
          err instanceof Error
            ? err.message
            : "Server bilan bog'lanib bo'lmadi."
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [router, retryKey]);

  const handleRetry = useCallback(() => {
    setError("");
    setLoading(true);
    setRetryKey((value) => value + 1);
  }, []);

  async function handleLogout() {
    if (busy) return;

    setBusy(true);
    setError("");

    try {
      await authApi.logout();
      router.replace("/login");
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "Tizimdan chiqishda xatolik yuz berdi."
      );
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <main className="min-h-screen bg-[var(--background)]">
        <LoadingState
          fullPage
          title="Dashboard yuklanmoqda"
          description="MedVision hisobingiz xavfsiz tekshirilmoqda."
        />
      </main>
    );
  }

  if (!user) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[var(--background)] px-4 py-10">
        <div className="w-full max-w-lg">
          <ErrorState
            title="Dashboard ochilmadi"
            description={error || "Tizimga kirish talab qilinadi."}
            onRetry={handleRetry}
          />
          <div className="mt-5 text-center">
            <Link
              href="/login"
              className="text-sm font-semibold text-[var(--primary)] hover:underline"
            >
              Kirish sahifasiga o&apos;tish
            </Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[var(--background)] px-4 py-10 sm:px-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <MedVisionLogo href="/dashboard" size="header" />

          <div className="flex flex-wrap items-center gap-3">
            <ThemeSwitcher />
            <button
              type="button"
              onClick={handleLogout}
              disabled={busy}
              className="rounded-xl border border-[var(--border)] bg-[var(--surface)] px-5 py-3 text-sm font-semibold text-[var(--foreground)] transition-colors hover:bg-[var(--surface-muted)] disabled:opacity-50"
            >
              {busy ? "Chiqilmoqda..." : "Tizimdan chiqish"}
            </button>
          </div>
        </header>

        <section className="med-card p-7 sm:p-10">
          <p className="mb-2 text-sm font-semibold text-[var(--primary)]">
            MEDVISION DASHBOARD
          </p>

          <h1 className="text-3xl font-extrabold text-[var(--foreground)]">
            Xush kelibsiz, {user.name}!
          </h1>

          <p className="mt-3 text-[var(--muted)]">
            Siz MedVision platformasiga muvaffaqiyatli kirdingiz.
          </p>

          <div className="mt-8 rounded-2xl bg-[var(--surface-muted)] p-5">
            <h2 className="mb-4 text-lg font-bold text-[var(--foreground)]">
              Shaxsiy ma&apos;lumotlar
            </h2>

            <p className="text-sm text-[var(--muted)]">Ism</p>
            <p className="mb-4 font-semibold text-[var(--foreground)]">
              {user.name}
            </p>

            <p className="text-sm text-[var(--muted)]">Email</p>
            <p className="break-all font-semibold text-[var(--foreground)]">
              {user.email}
            </p>
          </div>

          <div className="mt-8 rounded-2xl border border-[var(--border)] bg-[var(--surface-muted)] p-6 sm:p-8">
  <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
    <div>
      <p className="text-xs font-bold uppercase tracking-widest text-[var(--primary)]">
        MEDVISION ANALYSIS STUDIO
      </p>
      <h2 className="mt-2 text-xl font-bold text-[var(--foreground)]">
        Tibbiy tasvirlarni tahlil qilish
      </h2>
      <p className="mt-2 max-w-md text-sm leading-6 text-[var(--muted)]">
        PNG yoki JPEG tasvir yuklang va segmentatsiya jarayonini boshlang.
      </p>
    </div>

    <Link
      href="/dashboard/segment"
      className="med-button med-button-primary shrink-0"
    >
      Tahlilni boshlash
    </Link>
  </div>
</div>

<DashboardOverview />

          {error && (
            <div className="mt-6">
              <ErrorState
                title="Amalni bajarib bo'lmadi"
                description={error}
              />
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

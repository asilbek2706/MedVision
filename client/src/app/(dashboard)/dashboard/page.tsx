"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Activity,
  ArrowRight,
  ChevronRight,
  Clock3,
  LayoutDashboard,
  LogOut,
  ScanLine,
  ShieldCheck,
  Sparkles,
  UserRound,
} from "lucide-react";
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

const navigation = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard, active: true },
  { label: "Segmentatsiya", href: "/dashboard/segment", icon: ScanLine, active: false },
  { label: "Tahlillar tarixi", href: "/dashboard/history", icon: Clock3, active: false },
];

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let active = true;

    authApi.me()
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
          err instanceof Error ? err.message : "Server bilan bog'lanib bo'lmadi."
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
        err instanceof Error ? err.message : "Tizimdan chiqishda xatolik yuz berdi."
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
            <Link href="/login" className="text-sm font-semibold text-[var(--primary)] hover:underline">
              Kirish sahifasiga o'tish
            </Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-7xl space-y-7 px-5 py-7 sm:px-8">


          <section className="med-gradient relative overflow-hidden rounded-3xl p-7 text-white sm:p-10">
            <div className="pointer-events-none absolute -right-16 -top-20 size-72 rounded-full border-[35px] border-white/10" />
            <div className="pointer-events-none absolute -bottom-28 right-28 size-64 rounded-full bg-white/5" />

            <div className="relative z-10 flex flex-wrap items-end justify-between gap-6">
              <div className="max-w-xl">
                <p className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1.5 text-xs font-semibold">
                  <Sparkles size={14} />
                  MEDVISION WORKSPACE
                </p>
                <h2 className="mt-5 text-3xl font-extrabold tracking-tight sm:text-4xl">
                  Xush kelibsiz, {user.name}!
                </h2>
                <p className="mt-3 text-sm leading-7 text-white/85">
                  Tibbiy tasvirlarni tahlil qiling, segmentatsiya natijalarini
                  kuzating va barcha ishlaringizni bir joyda boshqaring.
                </p>
              </div>

              <Link
                href="/dashboard/segment"
                className="inline-flex items-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-bold text-teal-800 transition-transform hover:-translate-y-0.5"
              >
                <ScanLine size={18} />
                Yangi tahlil
                <ArrowRight size={17} />
              </Link>
            </div>
          </section>

          <div className="grid gap-4 md:grid-cols-2">
            <Link
              href="/dashboard/segment"
              className="group border border-[var(--border)] bg-[var(--surface)] shadow-sm flex items-center justify-between rounded-2xl p-5"
            >
              <div className="flex items-center gap-4">
                <div className="rounded-xl bg-[var(--primary-light)] p-3 text-[var(--primary)]">
                  <Activity size={23} />
                </div>
                <div>
                  <p className="font-bold text-[var(--foreground)]">Segmentation Studio</p>
                  <p className="mt-1 text-sm text-[var(--muted)]">Yangi tasvirni tahlil qilish</p>
                </div>
              </div>
              <ArrowRight size={19} className="text-[var(--muted)] transition-transform group-hover:translate-x-1" />
            </Link>

            <Link
              href="/dashboard/history"
              className="group border border-[var(--border)] bg-[var(--surface)] shadow-sm flex items-center justify-between rounded-2xl p-5"
            >
              <div className="flex items-center gap-4">
                <div className="rounded-xl bg-[var(--primary-light)] p-3 text-[var(--primary)]">
                  <Clock3 size={23} />
                </div>
                <div>
                  <p className="font-bold text-[var(--foreground)]">Analysis History</p>
                  <p className="mt-1 text-sm text-[var(--muted)]">Oldingi natijalarni ko'rish</p>
                </div>
              </div>
              <ArrowRight size={19} className="text-[var(--muted)] transition-transform group-hover:translate-x-1" />
            </Link>
          </div>

          <DashboardOverview />

          {error && (
            <ErrorState
              title="Amalni bajarib bo'lmadi"
              description={error}
            />
          )}
    </main>
  );
}
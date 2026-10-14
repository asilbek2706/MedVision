"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  ChevronRight,
  Clock3,
  LayoutDashboard,
  LogOut,
  Menu,
  ScanLine,
  ShieldCheck,
  X,
} from "lucide-react";

import { MedVisionLogo } from "@/components/ui/MedVisionLogo";
import { ThemeSwitcher } from "@/components/ui/ThemeSwitcher";
import { authApi, ApiRequestError } from "@/lib/api";

const navigation = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Segmentation Studio", href: "/dashboard/segment", icon: ScanLine },
  { label: "Analysis History", href: "/dashboard/history", icon: Clock3 },
];

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();

  const [authStatus, setAuthStatus] = useState<
    "checking" | "authenticated" | "unauthenticated" | "error"
  >("checking");

  const [authError, setAuthError] = useState("");
  const [authRetry, setAuthRetry] = useState(0);

  const [mobileOpen, setMobileOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState("");

  const current = navigation
    .filter(
      (item) =>
        pathname === item.href ||
        pathname.startsWith(item.href + "/"),
    )
    .sort((a, b) => b.href.length - a.href.length)[0];

  useEffect(() => {
    let active = true;

    authApi.me()
      .then(() => {
        if (active) setAuthStatus("authenticated");
      })
      .catch((error: unknown) => {
        if (!active) return;

        if (
          error instanceof ApiRequestError &&
          (error.status === 401 || error.status === 403)
        ) {
          setAuthStatus("unauthenticated");
          router.replace(
            "/login?next=" + encodeURIComponent(pathname),
          );
          return;
        }

        setAuthError(
          error instanceof Error
            ? error.message
            : "Server bilan bog'lanib bo'lmadi.",
        );
        setAuthStatus("error");
      });

    return () => {
      active = false;
    };
  }, [router, pathname, authRetry]);

  async function logout() {
    if (loggingOut) return;

    setLoggingOut(true);
    setLogoutError("");

    try {
      await authApi.logout();
      router.replace("/login");
    } catch (error) {
      setLogoutError(
        error instanceof Error
          ? error.message
          : "Tizimdan chiqishda xatolik yuz berdi.",
      );
      setLoggingOut(false);
    }
  }

  if (authStatus !== "authenticated") {
    if (authStatus === "error") {
      return (
        <main className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[var(--background)] px-6 text-center">
          <h1 className="text-xl font-bold text-[var(--foreground)]">
            Sessiyani tekshirib bo'lmadi
          </h1>
          <p className="max-w-md text-sm text-[var(--muted)]">
            {authError}
          </p>
          <button
            type="button"
            onClick={() => {
              setAuthStatus("checking");
              setAuthError("");
              setAuthRetry((value) => value + 1);
            }}
            className="rounded-2xl bg-teal-600 px-6 py-3 font-semibold text-white hover:bg-teal-700"
          >
            Qayta urinish
          </button>
        </main>
      );
    }

    return (
      <main
        role="status"
        aria-live="polite"
        className="flex min-h-screen items-center justify-center bg-[var(--background)]"
      >
        <div className="flex flex-col items-center gap-4">
          <div className="size-10 animate-spin rounded-full border-4 border-teal-500/20 border-t-teal-500" />
          <p className="text-sm font-medium text-[var(--muted)]">
            Xavfsiz sessiya tekshirilmoqda...
          </p>
        </div>
      </main>
    );
  }

  return (
    <div className="med-app-shell min-h-screen lg:flex">
      {mobileOpen && (
        <button
          type="button"
          aria-label="Menyuni yopish"
          onClick={() => setMobileOpen(false)}
          className="fixed inset-0 z-40 bg-black/40 lg:hidden"
        />
      )}

      <aside
        className={`med-app-sidebar fixed inset-y-0 left-0 z-50 flex w-64 flex-col transition-transform lg:sticky lg:top-0 lg:z-10 lg:h-screen lg:shrink-0 lg:translate-x-0 ${
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center justify-between border-b border-[(--border)] px-5 py-6">
          <MedVisionLogo href="/dashboard" size="header" />

          <button
            type="button"
            onClick={() => setMobileOpen(false)}
            aria-label="Menyuni yopish"
            className="rounded-lg p-2 text-[(--muted)] lg:hidden"
          >
            <X size={19} />
          </button>
        </div>

        <nav className="min-h-0 flex-1 space-y-1.5 overflow-y-auto px-4 py-7">


          {navigation.map(({ label, href, icon: Icon }) => {
            const active = current?.href === href;

            return (
              <Link
                key={href}
                href={href}
                aria-current={active ? "page" : undefined}
                onClick={() => setMobileOpen(false)}
                className={`flex items-center justify-between rounded-2xl px-4 py-3.5 text-sm font-semibold transition-all duration-200 ${
                  active
                    ? "bg-[var(--primary-light)] text-[var(--primary)]"
                    : "text-[var(--muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--foreground)]"
                }`}
              >
                <span className="flex items-center gap-3">
                  <Icon size={18} />
                  {label}
                </span>

                {active && <ChevronRight size={16} />}
              </Link>
            );
          })}

          <div className="mt-9 rounded-2xl border border-[var(--border)] bg-[var(--surface-muted)] p-4">
            <ShieldCheck size={21} className="text-[var(--primary)]" />
            <p className="mt-3 text-sm font-bold text-[var(--foreground)]">
              Medical Imaging
            </p>
            <p className="mt-2 text-xs leading-5 text-[var(--muted)]">
              Tibbiy tasvirlarni tahlil qilish va natijalarni boshqarish.
            </p>
          </div>
        </nav>

        <div className="shrink-0 border-t border-[var(--border)] p-4">
          <p className="text-xs font-semibold text-[var(--muted)]">
            MedVision Workspace
          </p>
          <p className="mt-1 text-xs text-[var(--primary)]">
            Secure analysis environment
          </p>
        </div>
      </aside>

      <div className="med-app-content min-w-0 flex-1">
        <header className="med-app-header sticky top-0 z-30">
          <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-4 px-5 py-5 sm:px-8">
            <div className="flex min-w-0 items-center gap-3">
              <button
                type="button"
                aria-label="Menyuni ochish"
                onClick={() => setMobileOpen(true)}
                className="rounded-xl border border-[var(--border)] p-2 text-[var(--foreground)] lg:hidden"
              >
                <Menu size={20} />
              </button>

              <div className="min-w-0">
                <p className="text-xs text-[var(--muted)]">
                  Workspace / {current?.label ?? "Analysis"}
                </p>
                <h1 className="truncate text-lg font-bold text-[var(--foreground)]">
                  {current?.label ?? "MedVision"}
                </h1>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <ThemeSwitcher />

              <button
                type="button"
                onClick={logout}
                disabled={loggingOut}
                className="inline-flex items-center gap-2 rounded-xl border border-[var(--border)] px-3 py-2.5 text-sm font-semibold text-[var(--foreground)] hover:bg-[var(--surface-muted)] disabled:opacity-50"
              >
                <LogOut size={16} />
                <span className="hidden sm:inline">
                  {loggingOut ? "Chiqilmoqda..." : "Chiqish"}
                </span>
              </button>
            </div>
          </div>

          {logoutError && (
            <p role="alert" className="px-5 pb-3 text-sm text-red-600">
              {logoutError}
            </p>
          )}
        </header>

        {children}
      </div>
    </div>
  );
}
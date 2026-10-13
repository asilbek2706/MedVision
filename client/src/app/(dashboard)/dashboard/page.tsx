"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
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
  }, [router]);

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
      <main className="flex min-h-screen items-center justify-center bg-[#f4f8fa]">
        <p role="status" className="text-slate-600">
          Yuklanmoqda...
        </p>
      </main>
    );
  }

  if (!user) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#f4f8fa] px-4">
        <section className="med-card w-full max-w-md p-8 text-center">
          <p role="alert" className="mb-4 text-slate-600">
            {error || "Tizimga kirish talab qilinadi."}
          </p>
          <Link href="/login" className="font-semibold text-teal-700">
            Kirish sahifasiga o'tish
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#f4f8fa] px-4 py-10 sm:px-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <Link href="/" className="text-xl font-extrabold text-teal-700">
            MedVision
          </Link>

          <button
            type="button"
            onClick={handleLogout}
            disabled={busy}
            className="rounded-xl border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            {busy ? "Chiqilmoqda..." : "Tizimdan chiqish"}
          </button>
        </header>

        <section className="med-card p-7 sm:p-10">
          <p className="mb-2 text-sm font-semibold text-teal-700">
            MEDVISION DASHBOARD
          </p>

          <h1 className="text-3xl font-extrabold text-slate-900">
            Xush kelibsiz, {user.name}!
          </h1>

          <p className="mt-3 text-slate-500">
            Siz MedVision platformasiga muvaffaqiyatli kirdingiz.
          </p>

          <div className="mt-8 rounded-2xl bg-slate-50 p-5">
            <h2 className="mb-4 text-lg font-bold text-slate-900">
              Shaxsiy ma'lumotlar
            </h2>

            <p className="text-sm text-slate-500">Ism</p>
            <p className="mb-4 font-semibold text-slate-900">{user.name}</p>

            <p className="text-sm text-slate-500">Email</p>
            <p className="break-all font-semibold text-slate-900">
              {user.email}
            </p>
          </div>

          {error && (
            <p role="alert" className="mt-5 text-sm text-red-600">
              {error}
            </p>
          )}
        </section>
      </div>
    </main>
  );
}
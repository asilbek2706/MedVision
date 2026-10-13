"use client";

import { useState, type FormEvent } from "react";
import { MedVisionLogo } from "@/components/ui/MedVisionLogo";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/api";
import { ThemeSwitcher } from "@/components/ui/ThemeSwitcher";

type AuthMode = "login" | "register";

export default function AuthForm({ mode }: { mode: AuthMode }) {
  const router = useRouter();
  const isRegister = mode === "register";

  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;

    const form = event.currentTarget;
    const fields = new FormData(form);

    const name = String(fields.get("name") ?? "").trim();
    const email = String(fields.get("email") ?? "").trim();
    const password = String(fields.get("password") ?? "");

    setBusy(true);
    setError("");

    try {
      if (isRegister) {
        await authApi.register(name, email, password);
        router.replace("/login?registered=1");
      } else {
        await authApi.login(email, password);
        await authApi.me();
        router.replace("/dashboard");
      }
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : "So'rovni bajarishda xatolik yuz berdi."
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="relative flex min-h-screen items-center justify-center bg-[var(--background)] px-4 py-12 text-[var(--foreground)]">
      <div className="absolute right-4 top-5 sm:right-8">
        <ThemeSwitcher />
      </div>
      <div className="w-full max-w-[440px]">
        <div className="mb-8 flex justify-center"><MedVisionLogo size="auth" /></div>

        <section className="med-card p-7 sm:p-10">
          <div className="mb-8 text-center">
            <h1 className="text-2xl font-extrabold tracking-tight text-[var(--foreground)]">
              {isRegister ? "Hisob yaratish" : "Tizimga kirish"}
            </h1>
            <p className="mt-2 text-sm leading-6 text-[var(--muted)]">
              {isRegister
                ? "MedVision platformasidan foydalanish uchun ro'yxatdan o'ting."
                : "MedVision hisobingizga kirish uchun ma'lumotlaringizni kiriting."}
            </p>
          </div>

          {!isRegister &&
            typeof window !== "undefined" &&
            new URLSearchParams(window.location.search).has("registered") && (
              <p
                role="status"
                className="mb-5 rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700"
              >
                Hisob yaratildi. Endi tizimga kiring.
              </p>
            )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {isRegister && (
              <label className="block">
                <span className="mb-2 block text-sm font-semibold text-[var(--foreground)]">
                  Ism
                </span>
                <input
                  name="name"
                  autoComplete="name"
                  className="med-input"
                  placeholder="Ismingiz"
                  maxLength={100}
                  required
                  disabled={busy}
                />
              </label>
            )}

            <label className="block">
              <span className="mb-2 block text-sm font-semibold text-[var(--foreground)]">
                Email
              </span>
              <input
                name="email"
                type="email"
                autoComplete="username"
                className="med-input"
                placeholder="example@email.com"
                maxLength={254}
                required
                disabled={busy}
              />
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-semibold text-[var(--foreground)]">
                Parol
              </span>
              <div className="relative">
                <input
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete={isRegister ? "new-password" : "current-password"}
                  className="med-input pr-12"
                  placeholder="Parolingiz"
                  minLength={isRegister ? 8 : 1}
                  maxLength={72}
                  required
                  disabled={busy}
                />

                <button
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                  aria-label={showPassword ? "Parolni yashirish" : "Parolni ko'rsatish"}
                  aria-pressed={showPassword}
                  title={showPassword ? "Parolni yashirish" : "Parolni ko'rsatish"}
                  className="absolute inset-y-0 right-0 flex w-12 items-center justify-center text-[var(--muted)] transition-colors hover:text-[var(--primary)] focus-visible:outline-2 focus-visible:outline-teal-600"
                >
                  {showPassword ? (
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      aria-hidden="true"
                    >
                      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  ) : (
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      aria-hidden="true"
                    >
                      <path d="M3 3l18 18" />
                      <path d="M10.6 5.1A11.5 11.5 0 0 1 12 5c6.5 0 10 7 10 7a16 16 0 0 1-3.1 3.8" />
                      <path d="M6.3 6.3C3.5 8.3 2 12 2 12s3.5 7 10 7a10.7 10.7 0 0 0 4.3-.9" />
                      <path d="M10 10a3 3 0 0 0 4 4" />
                    </svg>
                  )}
                </button>
              </div>
            </label>

            {error && (
              <p
                role="alert"
                className="rounded-xl bg-red-50 p-3 text-sm text-red-700"
              >
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={busy}
              className="med-button med-button-primary w-full disabled:opacity-60"
            >
              {busy
                ? "Kutilmoqda..."
                : isRegister
                  ? "Hisob yaratish"
                  : "Kirish"}
            </button>
          </form>

          <p className="mt-7 text-center text-sm text-[var(--muted)]">
            {isRegister ? "Hisobingiz bormi?" : "Hisobingiz yo'qmi?"}{" "}
            <Link
              href={isRegister ? "/login" : "/register"}
              className="font-bold text-[var(--primary)] hover:underline"
            >
              {isRegister ? "Kirish" : "Ro'yxatdan o'tish"}
            </Link>
          </p>
        </section>

        <p className="mt-6 text-center text-xs text-[var(--muted)]">
          MedVision - Medical Image Analysis
        </p>
      </div>
    </main>
  );
}


"use client";

import { useEffect, useState, type FormEvent } from "react";
import { authApi, ApiRequestError, type CurrentUser } from "@/lib/api";

export default function Home() {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [mode, setMode] = useState<"login" | "register">("login");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;

    authApi.me()
      .then((current) => {
        if (active) setUser(current);
      })
      .catch((error: unknown) => {
        if (
          active &&
          !(error instanceof ApiRequestError && error.status === 401)
        ) {
          setMessage("Server bilan bog'lanib bo'lmadi.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;

    const form = event.currentTarget;
    const fields = new FormData(form);
    const email = String(fields.get("email") ?? "");
    const password = String(fields.get("password") ?? "");

    setBusy(true);
    setMessage("");

    try {
      if (mode === "register") {
        await authApi.register(
          String(fields.get("name") ?? ""),
          email,
          password,
        );
        setMode("login");
        setMessage("Hisob yaratildi. Endi tizimga kiring.");
      } else {
        await authApi.login(email, password);
        // Confirm that authentication persisted to the session.
        setUser(await authApi.me());
      }
      form.reset();
    } catch (error: unknown) {
      setMessage(
        error instanceof Error ? error.message : "So'rov bajarilmadi.",
      );
    } finally {
      const passwordInput = form.elements.namedItem("password");
      if (passwordInput instanceof HTMLInputElement) {
        passwordInput.value = "";
      }
      setBusy(false);
    }
  }

  async function logout() {
    if (busy) return;
    setBusy(true);
    setMessage("");

    try {
      await authApi.logout();
      setUser(null);
      setMessage("Tizimdan chiqdingiz.");
    } catch (error: unknown) {
      setMessage(
        error instanceof Error ? error.message : "Chiqish bajarilmadi.",
      );
    } finally {
      setBusy(false);
    }
  }

  const inputClass =
    "w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100";

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 px-5 py-12 text-slate-900">
      <section className="w-full max-w-md rounded-3xl bg-white p-8 shadow-sm">
        <p className="mb-2 text-sm font-semibold tracking-widest text-teal-700">
          MEDVISION
        </p>
        <h1 className="mb-6 text-2xl font-bold">
          {user ? "Shaxsiy hisob" : mode === "login" ? "Tizimga kirish" : "Hisob yaratish"}
        </h1>

        {loading ? (
          <p role="status">Yuklanmoqda...</p>
        ) : user ? (
          <div className="space-y-4">
            <div>
              <p className="text-lg font-semibold">{user.name}</p>
              <p className="break-all text-slate-600">{user.email}</p>
            </div>
            <button
              type="button"
              disabled={busy}
              onClick={logout}
              className="w-full rounded-xl bg-slate-900 px-4 py-3 font-semibold text-white disabled:opacity-50"
            >
              {busy ? "Kutilmoqda..." : "Tizimdan chiqish"}
            </button>
          </div>
        ) : (
          <form onSubmit={submit} className="space-y-4">
            {mode === "register" && (
              <label className="block">
                <span className="mb-1 block text-sm">Ism</span>
                <input
                  className={inputClass}
                  name="name"
                  autoComplete="name"
                  maxLength={100}
                  required
                  disabled={busy}
                />
              </label>
            )}
            <label className="block">
              <span className="mb-1 block text-sm">Email</span>
              <input
                className={inputClass}
                name="email"
                type="email"
                autoComplete="username"
                maxLength={254}
                required
                disabled={busy}
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-sm">Parol</span>
              <input
                className={inputClass}
                name="password"
                type="password"
                autoComplete={mode === "register" ? "new-password" : "current-password"}
                minLength={mode === "register" ? 8 : 1}
                maxLength={72}
                required
                disabled={busy}
              />
            </label>
            <button
              type="submit"
              disabled={busy}
              className="w-full rounded-xl bg-teal-700 px-4 py-3 font-semibold text-white disabled:opacity-50"
            >
              {busy ? "Kutilmoqda..." : mode === "login" ? "Kirish" : "Hisob yaratish"}
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => {
                setMode(mode === "login" ? "register" : "login");
                setMessage("");
              }}
              className="w-full text-sm font-medium text-teal-700 disabled:opacity-50"
            >
              {mode === "login" ? "Yangi hisob yaratish" : "Kirish sahifasiga qaytish"}
            </button>
          </form>
        )}

        {message && (
          <p role="status" aria-live="polite" className="mt-5 text-sm text-slate-700">
            {message}
          </p>
        )}
      </section>
    </main>
  );
}
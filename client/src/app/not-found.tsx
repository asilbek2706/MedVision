import Link from "next/link";
import {
  ArrowLeft,
  ArrowRight,
  Home,
  ScanLine,
  ShieldCheck,
  SearchX,
} from "lucide-react";

import { MedVisionLogo } from "@/components/ui/MedVisionLogo";

export default function NotFound() {
  return (
    <main className="relative flex min-h-screen flex-col overflow-hidden bg-[var(--background)] text-[var(--foreground)]">
      <header className="relative z-10 mx-auto flex w-full max-w-7xl items-center justify-between px-6 py-7 sm:px-10">
        <MedVisionLogo href="/" size="header" />

        <span className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] bg-[var(--surface)] px-4 py-2 text-xs font-semibold text-[var(--muted)]">
          <ShieldCheck size={15} className="text-[var(--primary)]" />
          MedVision Platform
        </span>
      </header>

      <section className="relative z-10 mx-auto flex w-full max-w-5xl flex-1 flex-col items-center justify-center px-6 pb-24 pt-12 text-center">
        <div className="relative mb-9">
          <div className="absolute inset-0 scale-150 rounded-full bg-teal-500/10 blur-3xl" />

          <div className="relative flex size-24 items-center justify-center rounded-[30px] border border-[var(--border)] bg-[var(--surface)] shadow-xl shadow-black/5 sm:size-28">
            <SearchX
              size={48}
              strokeWidth={1.5}
              className="text-[var(--primary)]"
            />
          </div>
        </div>

        <div className="relative">
          <span className="select-none bg-gradient-to-r from-teal-400 via-cyan-500 to-teal-600 bg-clip-text text-[110px] font-black leading-none tracking-[-0.09em] text-transparent sm:text-[180px]">
            404
          </span>
        </div>

        <div className="mt-5 inline-flex items-center gap-2 rounded-full border border-[var(--border)] bg-[var(--surface)] px-4 py-2 text-xs font-bold uppercase tracking-[0.16em] text-[var(--primary)]">
          <span className="size-2 animate-pulse rounded-full bg-teal-400" />
          Page Not Found
        </div>

        <h1 className="mt-7 max-w-2xl text-3xl font-extrabold tracking-tight sm:text-5xl">
          Bu sahifa topilmadi
        </h1>

        <p className="mt-5 max-w-xl text-base leading-8 text-[var(--muted)] sm:text-lg">
          Siz izlayotgan sahifa mavjud emas, ko'chirilgan yoki
          manzili noto'g'ri kiritilgan bo'lishi mumkin.
        </p>

        <div className="mt-10 flex flex-wrap items-center justify-center gap-3">
          <Link
            href="/dashboard"
            className="inline-flex min-h-12 items-center justify-center gap-3 rounded-2xl bg-teal-600 px-6 py-3 text-sm font-bold text-white shadow-lg shadow-teal-900/15 transition-all hover:-translate-y-0.5 hover:bg-teal-700"
          >
            <Home size={18} />
            Dashboardga o'tish
            <ArrowRight size={17} />
          </Link>

          <Link
            href="/"
            className="inline-flex min-h-12 items-center justify-center gap-2 rounded-2xl border border-[var(--border)] bg-[var(--surface)] px-6 py-3 text-sm font-semibold text-[var(--foreground)] transition-colors hover:bg-[var(--surface-muted)]"
          >
            <ArrowLeft size={17} />
            Bosh sahifa
          </Link>
        </div>

        <div className="mt-14 grid w-full max-w-xl gap-4 sm:grid-cols-2">
          <Link
            href="/dashboard/segment"
            className="group flex items-center gap-4 rounded-3xl border border-[var(--border)] bg-[var(--surface)] p-5 text-left transition-all hover:border-teal-500/40 hover:shadow-lg hover:shadow-black/5"
          >
            <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-teal-500/10 text-[var(--primary)]">
              <ScanLine size={23} />
            </div>
            <div className="min-w-0">
              <p className="font-bold">Segmentation Studio</p>
              <p className="mt-1 text-xs text-[var(--muted)]">
                Yangi tahlil boshlash
              </p>
            </div>
            <ArrowRight
              size={17}
              className="ml-auto shrink-0 text-[var(--muted)] transition-transform group-hover:translate-x-1"
            />
          </Link>

          <Link
            href="/dashboard/history"
            className="group flex items-center gap-4 rounded-3xl border border-[var(--border)] bg-[var(--surface)] p-5 text-left transition-all hover:border-teal-500/40 hover:shadow-lg hover:shadow-black/5"
          >
            <div className="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-teal-500/10 text-[var(--primary)]">
              <ShieldCheck size={23} />
            </div>
            <div className="min-w-0">
              <p className="font-bold">Analysis History</p>
              <p className="mt-1 text-xs text-[var(--muted)]">
                Tahlillarni ko'rish
              </p>
            </div>
            <ArrowRight
              size={17}
              className="ml-auto shrink-0 text-[var(--muted)] transition-transform group-hover:translate-x-1"
            />
          </Link>
        </div>
      </section>

      <footer className="relative z-10 border-t border-[var(--border)] px-6 py-5 text-center text-xs text-[var(--muted)]">
        MedVision · Medical Image Intelligence
      </footer>
    </main>
  );
}
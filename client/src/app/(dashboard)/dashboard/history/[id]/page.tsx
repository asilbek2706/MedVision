import { Suspense } from "react";
import AnalysisDetailClient from "./AnalysisDetailClient";

export default function AnalysisDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="p-8 text-sm text-[var(--muted)]">
          Tahlil ma'lumotlari yuklanmoqda...
        </div>
      }
    >
      <AnalysisDetailClient />
    </Suspense>
  );
}
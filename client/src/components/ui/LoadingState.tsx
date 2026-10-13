type LoadingStateProps = {
  title?: string;
  description?: string;
  fullPage?: boolean;
};

export function LoadingState({
  title = "Yuklanmoqda...",
  description = "Ma'lumotlar tayyorlanmoqda. Iltimos, kuting.",
  fullPage = false,
}: LoadingStateProps) {
  return (
    <section
      role="status"
      aria-live="polite"
      className={`flex w-full flex-col items-center justify-center px-6 py-16 text-center ${
        fullPage ? "min-h-screen" : "min-h-64"
      }`}
    >
      <div className="relative mb-7 flex h-20 w-20 items-center justify-center">
        <div className="absolute inset-0 rounded-full border-4 border-[var(--border)]" />
        <div className="absolute inset-0 animate-spin rounded-full border-4 border-transparent border-t-[var(--primary)]" />
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--surface-muted)] text-[var(--primary)]">
          <svg
            width="25"
            height="25"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M12 3v18M3 12h18" />
            <circle cx="12" cy="12" r="9" />
          </svg>
        </div>
      </div>

      <h2 className="text-xl font-bold text-[var(--foreground)]">
        {title}
      </h2>
      <p className="mt-2 max-w-sm text-sm leading-6 text-[var(--muted)]">
        {description}
      </p>
    </section>
  );
}

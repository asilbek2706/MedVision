"use client";

type ErrorStateProps = {
  title?: string;
  description?: string;
  onRetry?: () => void;
  retryLabel?: string;
};

export function ErrorState({
  title = "Xatolik yuz berdi",
  description = "So'rovni bajarishda muammo yuz berdi. Qayta urinib ko'ring.",
  onRetry,
  retryLabel = "Qayta urinish",
}: ErrorStateProps) {
  return (
    <section
      role="alert"
      className="flex w-full flex-col items-center justify-center rounded-3xl border border-[var(--border)] bg-[var(--surface)] px-6 py-14 text-center shadow-sm"
    >
      <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-red-500/10 text-red-500">
        <svg
          width="32"
          height="32"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v6M12 17h.01" />
        </svg>
      </div>

      <h2 className="text-xl font-bold text-[var(--foreground)]">
        {title}
      </h2>
      <p className="mt-3 max-w-md text-sm leading-6 text-[var(--muted)]">
        {description}
      </p>

      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-7 inline-flex min-h-11 items-center justify-center rounded-xl bg-[var(--primary)] px-6 py-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--primary)]"
        >
          {retryLabel}
        </button>
      )}
    </section>
  );
}

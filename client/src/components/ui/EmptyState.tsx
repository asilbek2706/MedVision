"use client";

type EmptyStateProps = {
  title?: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function EmptyState({
  title = "Hozircha ma'lumot yo'q",
  description = "Bu yerda yangi ma'lumotlar paydo bo'lganda ko'rsatiladi.",
  actionLabel,
  onAction,
}: EmptyStateProps) {
  return (
    <section className="flex w-full flex-col items-center justify-center rounded-3xl border border-dashed border-[var(--border)] bg-[var(--surface)] px-6 py-14 text-center">
      <div className="mb-6 flex h-20 w-20 items-center justify-center rounded-3xl bg-[var(--surface-muted)] text-[var(--primary)]">
        <svg
          width="38"
          height="38"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M3 8.5 5 4h14l2 4.5V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8.5Z" />
          <path d="M3 12h5l2 3h4l2-3h5" />
        </svg>
      </div>

      <h2 className="text-xl font-bold text-[var(--foreground)]">
        {title}
      </h2>
      <p className="mt-3 max-w-md text-sm leading-6 text-[var(--muted)]">
        {description}
      </p>

      {actionLabel && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="mt-7 inline-flex min-h-11 items-center justify-center rounded-xl bg-[var(--primary)] px-6 py-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--primary)]"
        >
          {actionLabel}
        </button>
      )}
    </section>
  );
}

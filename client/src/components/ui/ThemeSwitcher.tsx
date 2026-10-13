"use client";

import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { useTheme } from "next-themes";


type ThemeMode = "light" | "dark" | "system";

const modes: { value: ThemeMode; label: string }[] = [
  { value: "light", label: "Light" },
  { value: "dark", label: "Dark" },
  { value: "system", label: "System" },
];

function Icon({ name }: { name: ThemeMode | "check" }) {
  const props = {
    width: 18,
    height: 18,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.8,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    "aria-hidden": true as const,
  };

  if (name === "light") {
    return (
      <svg {...props}>
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42" />
      </svg>
    );
  }

  if (name === "dark") {
    return (
      <svg {...props}>
        <path d="M20.9 13A9 9 0 0 1 11 3.1 9 9 0 1 0 20.9 13Z" />
      </svg>
    );
  }

  if (name === "system") {
    return (
      <svg {...props}>
        <rect x="3" y="4" width="18" height="14" rx="2" />
        <path d="M8 22h8m-4-4v4" />
      </svg>
    );
  }

  return (
    <svg {...props}>
      <path d="m5 12 4 4L19 6" />
    </svg>
  );
}

export function ThemeSwitcher() {
  const { theme, resolvedTheme, setTheme } = useTheme();
  const mounted = useSyncExternalStore(() => () => {}, () => true, () => false);
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);



  useEffect(() => {
    if (!open) return;

    function handlePointerDown(event: PointerEvent) {
      if (
        rootRef.current &&
        !rootRef.current.contains(event.target as Node)
      ) {
        setOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  const activeIcon =
    mounted && resolvedTheme === "dark" ? "dark" : "light";

  return (
    <div className="relative z-50 inline-flex shrink-0" ref={rootRef}>
      <button
        type="button"
        className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[var(--border)] bg-[var(--surface)] text-[var(--foreground)] shadow-sm transition-colors hover:bg-[var(--surface-muted)] focus-visible:outline-2 focus-visible:outline-[var(--primary)]"
        aria-label="Change appearance"
        aria-haspopup="true"
        aria-expanded={open}
        title="Appearance"
        disabled={!mounted}
        onClick={() => setOpen((value) => !value)}
      >
        <Icon name={activeIcon} />
      </button>

      {open && (
        <div className="absolute right-0 top-[calc(100%+10px)] z-[100] w-44 rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-1.5 text-[var(--foreground)] shadow-xl" role="group" aria-label="Appearance">
          <div className="px-3 py-2 text-[11px] font-bold uppercase tracking-widest text-[var(--muted)]">Appearance</div>

          {modes.map((mode) => {
            const selected = theme === mode.value;

            return (
              <button
                key={mode.value}
                type="button"
                className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm transition-colors hover:bg-[var(--surface-muted)] ${
                  selected ? "bg-[var(--primary-light)] text-[var(--primary)]" : "text-[var(--foreground)]"
                }`}
                aria-pressed={selected}
                onClick={() => {
                  setTheme(mode.value);
                  setOpen(false);
                }}
              >
                <Icon name={mode.value} />
                <span>{mode.label}</span>
                {selected && (
                  <span className="ml-auto inline-flex text-[var(--primary)]">
                    <Icon name="check" />
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

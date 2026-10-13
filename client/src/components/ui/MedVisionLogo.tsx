"use client";

import Image from "next/image";
import Link from "next/link";
import { useSyncExternalStore } from "react";
import { useTheme } from "next-themes";

type MedVisionLogoProps = {
  href?: string;
  className?: string;
  size?: "header" | "auth";
};

const subscribe = () => () => {};
const getClientSnapshot = () => true;
const getServerSnapshot = () => false;

export function MedVisionLogo({
  href = "/",
  className = "",
  size = "header",
}: MedVisionLogoProps) {
  const { resolvedTheme } = useTheme();
  const mounted = useSyncExternalStore(
    subscribe,
    getClientSnapshot,
    getServerSnapshot
  );

  const isDark = mounted && resolvedTheme === "dark";

  const imageClass =
    size === "auth"
      ? "h-auto w-[260px] max-w-full sm:w-[320px]"
      : "h-auto w-[240px] max-w-full sm:w-[280px]";

  return (
    <Link
      href={href}
      aria-label="MedVision bosh sahifasi"
      className={`inline-flex max-w-full shrink-0 items-center ${className}`}
    >
      <Image
        src={
          isDark
            ? "/medvision-logo-dark.svg"
            : "/medvision-logo.svg"
        }
        alt="MedVision"
        width={640}
        height={180}
        priority
        className={imageClass}
      />
    </Link>
  );
}

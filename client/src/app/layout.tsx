import type { Metadata, Viewport } from "next";

import "./globals.css";
import {ReactNode} from "react";

export const metadata: Metadata = {
  metadataBase: new URL("http://localhost:3000"),
  title: {
    default: "MedVision | Medical Image Analysis",
    template: "%s | MedVision",
  },
  description:
    "MedVision is a modern medical image analysis platform with image segmentation, visualization, and analysis management.",
  applicationName: "MedVision",
  keywords: [
    "MedVision",
    "Medical Imaging",
    "Image Segmentation",
    "Medical Image Analysis",
  ],
  icons: {
    icon: [
      {
        url: "/favicon.svg",
        type: "image/svg+xml",
      },
    ],
    shortcut: "/favicon.svg",
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#F4F8FA",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: ReactNode;
}>) {
  return (
    <html lang="uz" className="font-sans">
      <body className="min-h-screen antialiased">
        {children}
      </body>
    </html>
  );
}
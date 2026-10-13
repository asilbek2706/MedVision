import type { Metadata } from "next";
import AuthForm from "@/components/auth/AuthForm";

export const metadata: Metadata = {
  title: "Tizimga kirish",
};

export default function LoginPage() {
  return <AuthForm mode="login" />;
}
import type { Metadata } from "next";
import AuthForm from "@/components/auth/AuthForm";

export const metadata: Metadata = {
  title: "Ro'yxatdan o'tish",
};

export default function RegisterPage() {
  return <AuthForm mode="register" />;
}
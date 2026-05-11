import { API_BASE_URL } from "./http";
import type {
  AuthResponse,
  GoogleAuthRequest,
  RegisterPayload,
  RegisterResponse,
  SignInPayload,
  VerifyEmailPayload,
  VerifyEmailResponse,
} from "../types/auth";

const PLACEHOLDER_DELAY_MS = 900;
const VERIFY_CODE = "123456";
const RESEND_DELAY_MS = 700;

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}


export async function signInWithEmailAndPw(payload: SignInPayload): Promise<void> {
  await delay(PLACEHOLDER_DELAY_MS);

  if (payload.email.toLowerCase().includes("fail")) {
    throw new Error("Invalid email or password.");
  }
}

export async function registerAccount(
  payload: RegisterPayload
): Promise<RegisterResponse> {
  await delay(PLACEHOLDER_DELAY_MS);

  if (payload.email.toLowerCase().includes("taken")) {
    throw new Error("An account with this email already exists.");
  }

  return {
    email: payload.email,
    verificationExpiresInSeconds: 10 * 60,
  };
}

export async function verifyEmailCode(
  payload: VerifyEmailPayload
): Promise<VerifyEmailResponse> {
  await delay(PLACEHOLDER_DELAY_MS);

  if (payload.code === "000000") {
    throw new Error("This verification code has expired.");
  }

  if (payload.code !== VERIFY_CODE) {
    throw new Error("Invalid verification code.");
  }

  return { success: true };
}

export async function resendVerificationCode(email: string): Promise<void> {
  void email;
  await delay(RESEND_DELAY_MS);
}

// POSTs credentials to /auth/google
// Later, auth token should be stored in client-side for further api calls
export async function signInWithGoogle(
  payload: GoogleAuthRequest
): Promise<AuthResponse> {
  const path = "/auth/google";
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return (await response.json()) as AuthResponse;
}
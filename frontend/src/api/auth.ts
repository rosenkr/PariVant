import { API_BASE_URL } from "./http";
import type {
  AuthResponse,
  GoogleAuthRequest,
  AuthRequest,
} from "../types/auth";


export async function signInWithEmailAndPw(payload: AuthRequest): Promise<AuthResponse> {

  const path = "/auth/login";
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

export async function registerAccount(payload: AuthRequest): Promise<AuthResponse> {
  const path = "/auth/register";
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
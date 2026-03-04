// This file converts import.meta.env.VITE_API_BASE_URL into a usable constant for making HTTP requests.

const FALLBACK_LOCAL_API = "http://localhost:8080";

/**
 * Base URL for backend API calls.
 * Uses VITE_API_BASE_URL if set, otherwise defaults to localhost for dev.
 */
export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? FALLBACK_LOCAL_API;

if (!import.meta.env.VITE_API_BASE_URL) {
  console.warn(
    `VITE_API_BASE_URL is not set; defaulting to ${FALLBACK_LOCAL_API}`
  );
}
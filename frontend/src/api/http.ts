const FALLBACK_LOCAL_API = "http://localhost:8080";

const configuredApiBaseUrl =
  typeof import.meta.env.VITE_API_BASE_URL === "string"
    ? import.meta.env.VITE_API_BASE_URL
    : undefined;

/**
 * Base URL for backend API calls.
 * Uses VITE_API_BASE_URL if set, otherwise defaults to localhost for dev.
 */
export const API_BASE_URL = configuredApiBaseUrl ?? FALLBACK_LOCAL_API;

if (!configuredApiBaseUrl) {
  console.warn(
    `VITE_API_BASE_URL is not set; defaulting to ${FALLBACK_LOCAL_API}`
  );
}
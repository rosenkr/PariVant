// src/api/public/currentRound.ts
// Returns the "current" round from the backend endpoint /public/current.
// "Current" means: RUNNING if exists, else UPCOMING if exists, else 404.

import { API_BASE_URL } from "../http";
import type { CurrentRoundResponse, RoundType } from "../../types/round";

export type GetCurrentRoundResult =
  | { kind: "ok"; data: CurrentRoundResponse }
  | { kind: "no-round" } // backend returned 404
  | { kind: "server-error"; status: number; message: string } // backend returned non-404 error
  | { kind: "network-error"; message: string }; // fetch threw (network/CORS/etc.)

// /public/current returns (in this order): STRYKTIPSET running, else STRYKTIPSET upcoming,
// else EUROPATIPSET running, else EUROPATIPSET upcoming, else TOPPTIPSET running,
// else TOPPTIPSET upcoming, else 404.
export async function getCurrentRound(roundType?: RoundType): Promise<GetCurrentRoundResult> {
  const url = roundType
    ? `${API_BASE_URL}/public/current?roundType=${encodeURIComponent(roundType)}`
    : `${API_BASE_URL}/public/current`;

  try {
    const response = await fetch(url);

    if (response.status === 404) {
      return { kind: "no-round" };
    }

    if (!response.ok) {
      // Your backend error handler returns JSON with { message }, but we keep this generic:
      const text = await response.text().catch(() => "");
      const message = text || `HTTP ${response.status}`;
      return { kind: "server-error", status: response.status, message };
    }

    const data = (await response.json()) as CurrentRoundResponse;
    return { kind: "ok", data };
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Network error";
    return { kind: "network-error", message };
  }
}
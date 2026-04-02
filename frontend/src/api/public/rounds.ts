import { API_BASE_URL } from "../http";
import type { RoundStatus, RoundType, RoundView } from "../../types/round";

export type GetRoundsByFiltersResult =
  | { kind: "ok"; data: RoundView[] }
  | { kind: "server-error"; status: number; message: string }
  | { kind: "network-error"; message: string };

async function readErrorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => "");
  return text || `HTTP ${response.status}`;
}

/**
 * Expected backend contract:
 * GET /public/rounds?roundType=STRYKTIPSET&status=UPCOMING
 * -> RoundView[]
 *
 * The frontend owns the semantics of "which one is primary/current"
 * by selecting from the returned list.
 */
export async function getRoundsByFilters(
  roundType: RoundType,
  status: RoundStatus
): Promise<GetRoundsByFiltersResult> {
  const url =
    `${API_BASE_URL}/public/rounds` +
    `?roundType=${encodeURIComponent(roundType)}` +
    `&status=${encodeURIComponent(status)}`;

  try {
    const response = await fetch(url);

    if (!response.ok) {
      return {
        kind: "server-error",
        status: response.status,
        message: await readErrorMessage(response),
      };
    }

    const data = (await response.json()) as RoundView[];
    return { kind: "ok", data };
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Network error";
    return { kind: "network-error", message };
  }
}
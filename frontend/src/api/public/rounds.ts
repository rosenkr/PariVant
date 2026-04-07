import { API_BASE_URL } from "../http";
import type { RoundStatus, RoundType, RoundView } from "../../types/round";

export type GetRoundsResult =
  | { kind: "ok"; data: RoundView[] }
  | { kind: "server-error"; status: number; message: string }
  | { kind: "network-error"; message: string };

async function readErrorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => "");
  return text || `HTTP ${response.status}`;
}

export async function getRounds(
  status: RoundStatus,
  roundType?: RoundType
): Promise<GetRoundsResult> {
  const params = new URLSearchParams();
  params.set("status", status);
  if (roundType) {
    params.set("roundType", roundType);
  }

  const url = `${API_BASE_URL}/public/rounds?${params.toString()}`;

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
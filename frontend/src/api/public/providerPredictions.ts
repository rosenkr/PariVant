import { API_BASE_URL } from "../http";
import type {
  GetRoundProviderPredictionsResult,
  RoundProviderPredictionsResponse,
} from "../../types/providerPrediction";

export async function getRoundProviderPredictions(
  roundId: number
): Promise<GetRoundProviderPredictionsResult> {
  const url = `${API_BASE_URL}/public/rounds/${roundId}/provider-predictions`;

  try {
    const res = await fetch(url);

    if (!res.ok) {
      const text = await res.text().catch(() => "");
      return {
        kind: "server-error",
        status: res.status,
        message: text || `HTTP ${res.status}`,
      };
    }

    const data = (await res.json()) as RoundProviderPredictionsResponse;
    return { kind: "ok", data };
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : "Network error";
    return { kind: "network-error", message };
  }
}
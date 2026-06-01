import { API_BASE_URL } from "../http";
import type { GetModelRunsResult, ModelRunResponse } from "../../types/modelRun";

// Returns all model runs for a given round, ordered by generatedAt descending (most recent first).
export async function getModelRuns(roundId: number): Promise<GetModelRunsResult> {
  const url = `${API_BASE_URL}/public/rounds/${roundId}/model-runs`;

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

    const data = (await res.json()) as ModelRunResponse[];
    return { kind: "ok", data };
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : "Network error";
    return { kind: "network-error", message };
  }
}

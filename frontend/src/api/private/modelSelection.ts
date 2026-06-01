import { API_BASE_URL } from "../http";
import type {
  ModelSelectionRequest,
  ModelResultResponse,
} from "../../types/modelSelection";

async function readErrorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => "");
  return text || `HTTP ${response.status}`;
}

export async function runModelSelection(
  token: string,
  payload: ModelSelectionRequest,
): Promise<ModelResultResponse> {
  const response = await fetch(`${API_BASE_URL}/model/selection`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as ModelResultResponse;
}

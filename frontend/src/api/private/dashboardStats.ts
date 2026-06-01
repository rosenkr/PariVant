import { API_BASE_URL } from "../http";
import type { DashboardStatsResponse } from "../../types/dashboardStats";

async function readErrorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => "");
  return text || `HTTP ${response.status}`;
}

export async function getDashboardStats(
  token: string,
): Promise<DashboardStatsResponse> {
  const response = await fetch(`${API_BASE_URL}/dashboard/stats`, {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as DashboardStatsResponse;
}

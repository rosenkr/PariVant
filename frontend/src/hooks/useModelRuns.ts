import { useQuery } from "@tanstack/react-query";
import { getModelRuns } from "../api/public/modelRuns";
import type { GetModelRunsResult } from "../types/modelRun";
// Custom hook to fetch model runs for a given round ID. Returns the React Query result object.
export function useModelRuns(roundId: number | null) {
  return useQuery<GetModelRunsResult>({
    queryKey: ["modelRuns", roundId],
    queryFn: () => getModelRuns(roundId as number),
    enabled: roundId != null,
  });
}
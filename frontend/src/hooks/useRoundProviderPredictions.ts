import { useQuery } from "@tanstack/react-query";
import { getRoundProviderPredictions } from "../api/public/providerPredictions";

export function useRoundProviderPredictions(roundId: number | null) {
  return useQuery({
    queryKey: ["roundProviderPredictions", roundId],
    queryFn: () => getRoundProviderPredictions(roundId as number),
    enabled: roundId != null,
  });
}
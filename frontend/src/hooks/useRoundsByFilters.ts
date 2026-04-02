import { useQuery } from "@tanstack/react-query";
import { getRoundsByFilters } from "../api/public/rounds";
import type { RoundStatus, RoundType } from "../types/round";

export function useRoundsByFilters(roundType: RoundType, status: RoundStatus) {
  return useQuery({
    queryKey: ["roundsByFilters", roundType, status],
    queryFn: () => getRoundsByFilters(roundType, status),
    staleTime: 60 * 1000 * 5,
    retry: false,
  });
}
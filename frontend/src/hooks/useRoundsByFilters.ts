import { useQuery } from "@tanstack/react-query";
import { getRounds } from "../api/public/rounds";
import type { RoundStatus, RoundType } from "../types/round";

export function useRoundsByFilters(status: RoundStatus, roundType?: RoundType) {
  return useQuery({
    queryKey: ["rounds", status, roundType ?? "ALL"],
    queryFn: () => getRounds(status, roundType),
    staleTime: 1000 * 60 * 5,
    retry: false,
  });
}
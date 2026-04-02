import { getCurrentRound } from "../api/public/currentRound";
import { useQuery } from "@tanstack/react-query";
import type { RoundType } from "../types/round";

export function useCurrentRound(roundType?: RoundType) {
    return useQuery({
        queryKey: ["currentRound", roundType],
        queryFn: () => getCurrentRound(roundType),
        staleTime: 60 * 1000 * 10, // 10 minutes,
        retry: false, // Don't retry on failure; the error will be handled in the UI
    });
}

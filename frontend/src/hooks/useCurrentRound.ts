import { getCurrentRound } from "../api/public/currentRound";
import { useQuery } from "@tanstack/react-query";
import type { GameType } from "../types/round";

export function useCurrentRound(gameType?: GameType) {
    return useQuery({
        queryKey: ["currentRound", gameType],
        queryFn: () => getCurrentRound(gameType),
        staleTime: 60 * 1000 * 10, // 10 minutes,
        retry: false, // Don't retry on failure; the error will be handled in the UI
    });
}

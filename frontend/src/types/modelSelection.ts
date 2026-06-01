import type { ModelResultResponse } from "./modelRun";
import type { ProbabilityTriple } from "./probabilityTriple";
import type { RoundType } from "./round";

export type ModelSelectionRequest = {
  roundType: RoundType;
  roundStartDate: string;
  matches: Array<{
    matchNumber: number;
    startDate: string;
    homeTeamName: string;
    awayTeamName: string;
  }>;
  budgetInSek: number;
  contexts: Record<
    number,
    {
      market: ProbabilityTriple;
      publicPick: ProbabilityTriple;
      providers: ProbabilityTriple[];
    }
  >;
  interventions?: Record<string, never>;
};

export type { ModelResultResponse };

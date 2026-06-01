import type { Outcome } from "./modelRun";
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

export type ModelSelectionResponse = {
  modelName: string;
  generatedAt: string;
  totalCostInSek: number;
  halfGuardsCount: number;
  selections: Record<string, Outcome[]>;
  internalProbabilities: Record<string, ProbabilityTriple>;
};

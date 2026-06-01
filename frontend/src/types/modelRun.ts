export type Outcome = "HOME_WIN" | "DRAW" | "AWAY_WIN";
import type { ProbabilityTriple } from "./probabilityTriple";

export type ModelResultResponse = {
  modelName: string;
  generatedAt: string;
  totalCostInSek: number;
  halfGuardsCount: number;
  fullGuardsCount: number;
  selections: Record<string, Outcome[]>;
  basePicks: Record<string, Outcome>;
  internalProbabilities: Record<string, ProbabilityTriple>;
};

export type ModelRunResponse = {
  id: number;
  budgetInSek: number;
  trigger: string;
  result: ModelResultResponse;
};

export type GetModelRunsResult =
  | { kind: "ok"; data: ModelRunResponse[] }
  | { kind: "server-error"; status: number; message: string }
  | { kind: "network-error"; message: string };

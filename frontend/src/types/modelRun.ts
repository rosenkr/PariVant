export type Outcome = "HOME_WIN" | "DRAW" | "AWAY_WIN";

export type ModelRunTrigger = "OPENED" | "T_MINUS_15" | "MANUAL";

export type ProbabilityTripleDtoShape = {
  homeWin: number;
  draw: number;
  awayWin: number;
};

export type ModelRunView = {
  id: number;
  modelName: string;
  generatedAt: string;
  budgetInSek: number;
  totalCostInSek: number;
  halfGuardsCount: number;
  fullGuardsCount?: number;
  trigger: ModelRunTrigger;

  selections?: Record<string, Outcome[]>;
  selectionsJson?: string;

  internalProbabilities?: Record<string, ProbabilityTripleDtoShape>;
  internalProbabilitiesJson?: string;

  weights?: {
    recentFormWeight?: number;
  };
  weightsJson?: string;

  decisionParameters?: {
    maxFullGuardsTopptipset?: number;
    maxFullGuardsStryktipset?: number;
    valueThresholdTopptipset?: number;
    valueThresholdStryktipset?: number;
    probabilityFloorTopptipset?: number;
    probabilityFloorStryktipset?: number;
  };
  decisionParametersJson?: string;
};

export type GetModelRunsResult =
  | { kind: "ok"; data: ModelRunView[] }
  | { kind: "server-error"; status: number; message: string }
  | { kind: "network-error"; message: string };
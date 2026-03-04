export type Outcome = "HOME_WIN" | "DRAW" | "AWAY_WIN";

export type ModelRunTrigger = "OPENED" | "T_MINUS_15" | "MANUAL";

// This type represents the data structure of a model run as returned by the backend API.
export type ModelRunView = {
  id: number;
  modelName: string;
  generatedAt: string; // ISO string
  budgetInSek: number;
  totalCostInSek: number;
  halfGuardsCount: number;
  fullGuardsCount: number;
  trigger: ModelRunTrigger;

  // Backend returns keys as strings in JSON ("1", "2", ...)
  selections: Record<string, Outcome[]>;

  weights?: {
    recentFormWeight?: number;
  };

  decisionParameters?: {
    maxFullGuardsTopptipset?: number;
    maxFullGuardsStryktipset?: number;
    valueThresholdTopptipset?: number;
    valueThresholdStryktipset?: number;
    probabilityFloorTopptipset?: number;
    probabilityFloorStryktipset?: number;
  };
};

export type GetModelRunsResult =
  | { kind: "ok"; data: ModelRunView[] }
  | { kind: "server-error"; status: number; message: string }
  | { kind: "network-error"; message: string };
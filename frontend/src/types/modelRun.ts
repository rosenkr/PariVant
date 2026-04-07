export type Outcome = "HOME_WIN" | "DRAW" | "AWAY_WIN";

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
  trigger: string;

  selectionsJson?: string;
  basePicksJson?: string;
  internalProbabilitiesJson?: string;

  selections?: Record<string, Outcome[]>;
  basePicks?: Record<string, Outcome>;
  internalProbabilities?: Record<string, ProbabilityTripleDtoShape>;
};
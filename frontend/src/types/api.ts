export type GameType = "TOPPTIPSET" | "STRYKTIPSET" | "EUROPATIPSET";

export type RoundStatus = "UPCOMING" | "RUNNING";

export type MatchView = {
  matchNumber: number;
  startDate: string;
  homeTeamName: string;
  awayTeamName: string;
};

export type RoundView = {
  id: number;
  startDate: string;
  endDate: string;
  matches: MatchView[];
};

export type CurrentRoundResponse = {
  selectedGameType: GameType;
  roundStatus: RoundStatus;
  round: RoundView;
};

export type Outcome = "HOME_WIN" | "DRAW" | "AWAY_WIN";
export type ModelRunTrigger = "OPENED" | "T_MINUS_15";

export type ModelRunView = {
  id: number;
  modelName: string;
  generatedAt: string;
  budgetInSek: number;
  totalCostInSek: number;
  halfGuardsCount: number;
  fullGuardsCount: number;
  trigger: ModelRunTrigger;

  // JSON object from backend: keys are match numbers as strings ("1", "2", ...)
  selections: Record<string, Outcome[]>;

  // kept as loose objects for now; we only display a couple fields
  weights: { recentFormWeight: number };
  decisionParameters: {
    maxFullGuardsTopptipset: number;
    maxFullGuardsStryktipset: number;
    valueThresholdTopptipset: number;
    valueThresholdStryktipset: number;
    probabilityFloorTopptipset: number;
    probabilityFloorStryktipset: number;
  };
};
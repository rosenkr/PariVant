export type ProviderPredictionView = {
  providerName: string;
  status: string;
  message: string | null;
  probabilityHome: number | null;
  probabilityDraw: number | null;
  probabilityAway: number | null;
};

export type MatchProviderPredictionsView = {
  matchNumber: number;
  providers: ProviderPredictionView[];
};

export type RoundProviderPredictionsResponse = {
  roundId: number;
  modelRunId: number | null;
  generatedAt: string | null;
  matches: MatchProviderPredictionsView[];
};

export type GetRoundProviderPredictionsResult =
  | { kind: "ok"; data: RoundProviderPredictionsResponse }
  | { kind: "server-error"; status: number; message: string }
  | { kind: "network-error"; message: string };
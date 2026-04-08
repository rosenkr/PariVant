export type RoundType = "TOPPTIPSET" | "STRYKTIPSET" | "EUROPATIPSET";

export type RoundStatus = "UPCOMING" | "RUNNING" | "ENDED";

export type TripleView = {
  homeWin: number;
  draw: number;
  awayWin: number;
};

export type MatchView = {
  matchNumber: number;
  startTime: string;
  homeTeamName: string;
  awayTeamName: string;
  market: TripleView | null;
  publicPick: TripleView | null;
  marketFallbackUsed: boolean;
  marketFallbackReason: string | null;
};

export type RoundView = {
  id: number;
  roundType: RoundType;
  startTime: string;
  matches: MatchView[];
};
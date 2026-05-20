import type { ProbabilityTriple } from "./probabilityTriple";

export type RoundType = "TOPPTIPSET" | "STRYKTIPSET" | "EUROPATIPSET";

export type RoundStatus = "UPCOMING" | "RUNNING" | "ENDED";

export type MatchView = {
  matchNumber: number;
  startTime: string;
  homeTeamName: string;
  awayTeamName: string;
  market: ProbabilityTriple | null;
  publicPick: ProbabilityTriple | null;
  marketFallbackUsed: boolean;
  marketFallbackReason: string | null;
};

export type RoundView = {
  id: number;
  roundType: RoundType;
  startTime: string;
  matches: MatchView[];
};

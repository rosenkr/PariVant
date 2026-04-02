/* Contains the necessary types to represent a round as fetched from the backend endpoint /public/current */

export type RoundType = "TOPPTIPSET" | "STRYKTIPSET" | "EUROPATIPSET";

export type RoundStatus = "UPCOMING" | "RUNNING";

export type TripleView = {
  homeWin: number; // 0..1
  draw: number; // 0..1
  awayWin: number; // 0..1
};

export type MatchView = {
  matchNumber: number;
  startDate: string; // ISO date string
  homeTeamName: string;
  awayTeamName: string;

  // NEW (Option B): include contexts inline
  market: TripleView | null;
  publicPick: TripleView | null;
};

export type RoundView = {
  id: number;
  startDate: string;
  matches: MatchView[];
};

export type CurrentRoundResponse = {
  selectedRoundType: RoundType;
  roundStatus: RoundStatus;
  round: RoundView;
};
/* Public round types used by the frontend when fetching rounds by filters. */

export type RoundType = "TOPPTIPSET" | "STRYKTIPSET" | "EUROPATIPSET";

export type RoundStatus = "UPCOMING" | "RUNNING" | "ENDED";
export type TripleView = {
  homeWin: number;
  draw: number;
  awayWin: number;
};

export type MatchView = {
  matchNumber: number;
  startDate: string;
  homeTeamName: string;
  awayTeamName: string;
  market: TripleView | null;
  publicPick: TripleView | null;
};

export type RoundView = {
  id: number;
  startDate: string;
  matches: MatchView[];
};
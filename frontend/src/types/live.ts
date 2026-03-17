// src/types/live.ts
// Types for the SSE /public/rounds/{roundId}/live endpoint.

export type LiveMatchUpdate = {
  matchNumber: number;
  fixtureId: number | null;
  homeGoals: number | null;
  awayGoals: number | null;
  status: string | null; // e.g. "1H", "2H", "HT", "FT"
  minute: number | null; // elapsed minutes (if provided)
};

export type LiveRoundSnapshot = {
  roundId: number;
  updatedAt: string; // ISO string
  matches: LiveMatchUpdate[];
};
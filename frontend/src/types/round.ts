/* Contains the necessary types to represent a round as fetched from the backend endpoint public/current */

export type GameType = 'TOPPTIPSET' | 'STRYKTIPSET' | 'EUROPATIPSET';

// Conceptually a round should also have status ENDED, but the backend doesn't return that information yet (not relevant for public/current endpoint which this type is initially defined for). In theory we can just add | ENDED for now.
export type RoundStatus = 'UPCOMING' | 'RUNNING';

export type RoundView = {
  id: number;
  startDate: string; // JSON date string
  endDate: string; // JSON date string
  matches: MatchView[];
};

export type MatchView = {
    matchNumber: number;
    startDate: string; // Can be later than the round's startDate
    homeTeamName: string;
    awayTeamName: string;
}

export type CurrentRoundResponse = {
    selectedGameType: GameType;
    roundStatus: RoundStatus;
    round: RoundView;
}
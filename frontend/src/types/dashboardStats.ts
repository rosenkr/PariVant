export type DashboardStatBlock = {
  wins: number;
  losses: number;
  undetermined: number;
};

export type DashboardStatsResponse = {
  couponStats: DashboardStatBlock;
  confidentPickStats: DashboardStatBlock;
};

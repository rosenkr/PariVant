import type { Outcome } from "./modelRun";
import type { RoundType } from "./round";

export type CouponStatus = "UNDETERMINED" | "WIN" | "LOSE";

export type CouponSelections = Record<string, Outcome[]>;

export type CouponRequest = {
  roundId: number;
  selections: CouponSelections;
  confidentPickMatchNumber: number | null;
};

export type CouponResponse = {
  id: number;
  roundId: number;
  roundType: RoundType;
  status: CouponStatus;
  correctPickCount: number | null;
  confidentPickMatchNumber: number | null;
  totalCost: number;
  selections: CouponSelections;
  createdAt: string;
  updatedAt: string;
};

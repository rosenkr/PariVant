import { API_BASE_URL } from "../http";
import type { CouponRequest, CouponResponse } from "../../types/coupon";

async function readErrorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => "");
  return text || `HTTP ${response.status}`;
}

function authHeaders(token: string): HeadersInit {
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
}

export async function getCoupons(token: string): Promise<CouponResponse[]> {
  const response = await fetch(`${API_BASE_URL}/coupons`, {
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as CouponResponse[];
}

export async function getCoupon(
  token: string,
  couponId: number,
): Promise<CouponResponse> {
  const response = await fetch(`${API_BASE_URL}/coupons/${couponId}`, {
    headers: authHeaders(token),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as CouponResponse;
}

export async function createCoupon(
  token: string,
  payload: CouponRequest,
): Promise<CouponResponse> {
  const response = await fetch(`${API_BASE_URL}/coupons`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return (await response.json()) as CouponResponse;
}

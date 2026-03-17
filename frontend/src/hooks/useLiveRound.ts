// src/hooks/useLiveRound.ts
import { useEffect, useMemo, useState } from "react";
import type { LiveRoundSnapshot } from "../types/live";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

type LiveState =
  | { status: "idle" }
  | { status: "connecting" }
  | { status: "live"; snapshot: LiveRoundSnapshot }
  | { status: "error"; message: string };

export function useLiveRound(roundId: number | null, enabled: boolean) {
  const [state, setState] = useState<LiveState>({ status: "idle" });

  useEffect(() => {
    if (!enabled || roundId == null) {
      setState({ status: "idle" });
      return;
    }

    setState({ status: "connecting" });

    const url = `${API_BASE_URL}/public/rounds/${roundId}/live`;
    const es = new EventSource(url);

    const onSnapshot = (ev: MessageEvent) => {
      try {
        const parsed = JSON.parse(ev.data) as LiveRoundSnapshot;
        setState({ status: "live", snapshot: parsed });
      } catch (e) {
        setState({ status: "error", message: "Failed to parse live snapshot JSON." });
      }
    };

    es.addEventListener("snapshot", onSnapshot as EventListener);

    es.onerror = () => {
      // Browser auto-retries EventSource. We keep a friendly state.
      setState({ status: "error", message: "Live connection lost. Reconnecting…" });
    };

    return () => {
      es.removeEventListener("snapshot", onSnapshot as EventListener);
      es.close();
    };
  }, [roundId, enabled]);

  // Convenience: return snapshot (or null) + state.
  const snapshot = useMemo(() => {
    return state.status === "live" ? state.snapshot : null;
  }, [state]);

  return { state, snapshot };
}
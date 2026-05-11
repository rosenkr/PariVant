import { useEffect, useMemo, useState } from "react";
import { API_BASE_URL } from "../api/http";
import type { LiveRoundSnapshot } from "../types/liveScore";

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

    const onSnapshot = (event: Event) => {
      if (!(event instanceof MessageEvent) || typeof event.data !== "string") {
        setState({
          status: "error",
          message: "Failed to parse live snapshot JSON.",
        });
        return;
      }

      try {
        const parsed = JSON.parse(event.data) as LiveRoundSnapshot;
        setState({ status: "live", snapshot: parsed });
      } catch {
        setState({
          status: "error",
          message: "Failed to parse live snapshot JSON.",
        });
      }
    };

    es.addEventListener("snapshot", onSnapshot);

    es.onerror = () => {
      setState({ status: "error", message: "Live connection lost. Reconnecting…" });
    };

    return () => {
      es.removeEventListener("snapshot", onSnapshot);
      es.close();
    };
  }, [roundId, enabled]);

  const snapshot = useMemo(() => {
    return state.status === "live" ? state.snapshot : null;
  }, [state]);

  return { state, snapshot };
}
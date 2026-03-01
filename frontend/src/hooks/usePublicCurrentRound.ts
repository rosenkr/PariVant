import { useEffect, useRef, useState } from "react";
import type { CurrentRoundResponse, GameType } from "../types/api";

type State =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "no-round" } // 404 from backend => no current/upcoming round
  | { status: "error"; message: string }
  | { status: "ok"; data: CurrentRoundResponse };

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

// Small helper: schedule state updates outside the effect's synchronous call stack
function enqueueStateUpdate(fn: () => void) {
  queueMicrotask(fn);
}

export function usePublicCurrentRound(gameType: GameType) {
  const [state, setState] = useState<State>({ status: "idle" });

  // Prevent setState after unmount / stale request
  const requestSeq = useRef(0);

  useEffect(() => {
    const seq = ++requestSeq.current;
    const controller = new AbortController();

    enqueueStateUpdate(() => setState({ status: "loading" }));

    void (async () => {
      try {
        const url = `${API_BASE_URL}/public/current?gameType=${encodeURIComponent(gameType)}`;
        const res = await fetch(url, { signal: controller.signal });

        if (seq !== requestSeq.current) return; // stale response

        if (res.status === 404) {
          enqueueStateUpdate(() => setState({ status: "no-round" }));
          return;
        }

        if (!res.ok) {
          const text = await res.text().catch(() => "");
          enqueueStateUpdate(() =>
            setState({
              status: "error",
              message: `GET /public/current failed (${res.status})${text ? `: ${text}` : ""}`,
            })
          );
          return;
        }

        const data: unknown = await res.json();

        // Minimal runtime shape check (avoid 'any' and still be safe-ish)
        if (
          typeof data === "object" &&
          data !== null &&
          "round" in data &&
          "roundStatus" in data &&
          "selectedGameType" in data
        ) {
          enqueueStateUpdate(() => setState({ status: "ok", data: data as CurrentRoundResponse }));
        } else {
          enqueueStateUpdate(() =>
            setState({ status: "error", message: "Unexpected response shape from backend." })
          );
        }
      } catch (err: unknown) {
        if (controller.signal.aborted) return;

        const msg =
          err instanceof Error ? err.message : "Unknown error while fetching /public/current";
        enqueueStateUpdate(() => setState({ status: "error", message: msg }));
      }
    })();

    return () => controller.abort();
  }, [gameType]);

  return state;
}
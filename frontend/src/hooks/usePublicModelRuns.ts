import { useEffect, useRef, useState } from "react";
import type { ModelRunView } from "../types/api";

type State =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ok"; data: ModelRunView[] };

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

// Defers state updates so we don't call setState synchronously inside useEffect.
function deferSetState<T>(setState: React.Dispatch<React.SetStateAction<T>>, next: T) {
  queueMicrotask(() => setState(next));
}

export function usePublicModelRuns(roundId: number | null) {
  const [state, setState] = useState<State>({ status: "idle" });
  const requestSeq = useRef(0);

  useEffect(() => {
    if (roundId == null) {
      deferSetState(setState, { status: "idle" });
      return;
    }

    const seq = ++requestSeq.current;
    const controller = new AbortController();

    deferSetState(setState, { status: "loading" });

    void (async () => {
      try {
        const url = `${API_BASE_URL}/public/rounds/${roundId}/model-runs`;
        const res = await fetch(url, { signal: controller.signal });

        if (seq !== requestSeq.current) return;

        if (!res.ok) {
          const text = await res.text().catch(() => "");
          deferSetState(setState, {
            status: "error",
            message: `GET /public/rounds/${roundId}/model-runs failed (${res.status})${
              text ? `: ${text}` : ""
            }`,
          });
          return;
        }

        const data = (await res.json()) as ModelRunView[];
        deferSetState(setState, { status: "ok", data });
      } catch (err: unknown) {
        if (controller.signal.aborted) return;
        const msg = err instanceof Error ? err.message : "Unknown error while fetching model runs";
        deferSetState(setState, { status: "error", message: msg });
      }
    })();

    return () => controller.abort();
  }, [roundId]);

  return state;
}
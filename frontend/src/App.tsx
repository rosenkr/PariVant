// frontend/src/App.tsx
import { useMemo, useState } from "react";
import type { GameType, ModelRunView, Outcome } from "./types/api";
import { usePublicCurrentRound } from "./hooks/usePublicCurrentRound";
import { usePublicModelRuns } from "./hooks/usePublicModelRuns";

const GAME_TYPES: GameType[] = ["STRYKTIPSET", "EUROPATIPSET", "TOPPTIPSET"];

const PRESET_BUDGETS = [32, 64, 128, 256] as const;
type PresetBudget = (typeof PRESET_BUDGETS)[number];

function parsePresetBudget(value: string): PresetBudget {
  const n = Number(value);
  if (n === 32 || n === 64 || n === 128 || n === 256) return n;
  return 64;
}

function outcomeLabel(o: Outcome) {
  if (o === "HOME_WIN") return "1";
  if (o === "DRAW") return "X";
  return "2";
}

function isSelected(
  selections: Record<string, Outcome[]>,
  matchNumber: number,
  outcome: Outcome,
) {
  const key = String(matchNumber);
  const arr = selections[key] ?? [];
  return arr.includes(outcome);
}

function pickRunForBudget(runs: ModelRunView[], budget: PresetBudget): ModelRunView | null {
  const exact = runs.find((r) => r.budgetInSek === budget);
  return exact ?? (runs.length > 0 ? runs[0] : null);
}

export default function App() {
  const [gameType, setGameType] = useState<GameType>("STRYKTIPSET");
  const [budget, setBudget] = useState<PresetBudget>(64);

  const current = usePublicCurrentRound(gameType);

  const roundId = current.status === "ok" ? current.data.round.id : null;
  const modelRuns = usePublicModelRuns(roundId);

  const title = useMemo(() => `Betting Model (MVP) — ${gameType}`, [gameType]);

  const selectedRun = useMemo(() => {
    if (modelRuns.status !== "ok") return null;
    return pickRunForBudget(modelRuns.data, budget);
  }, [modelRuns, budget]);

  return (
    <div
      style={{
        maxWidth: 1000,
        margin: "0 auto",
        padding: 24,
        fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, Arial",
      }}
    >
      <h1 style={{ marginTop: 0, marginBottom: 12 }}>{title}</h1>

      <div style={{ display: "flex", gap: 16, alignItems: "center", marginBottom: 16 }}>
        <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
          Game:
          <select value={gameType} onChange={(e) => setGameType(e.target.value as GameType)}>
            {GAME_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>

        <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
          Budget:
          <select value={budget} onChange={(e) => setBudget(parsePresetBudget(e.target.value))}>
            {PRESET_BUDGETS.map((b) => (
              <option key={b} value={b}>
                {b} SEK
              </option>
            ))}
          </select>
        </label>

        <div style={{ color: "rgba(255,255,255,0.60)" }}>
          Public page reads existing runs (no run-trigger from UI).
        </div>
      </div>

      {/* CURRENT ROUND */}
      {current.status === "loading" && <div>Loading current/upcoming round…</div>}

      {current.status === "no-round" && (
        <div style={{ padding: 12, border: "1px solid #444", borderRadius: 10 }}>
          <strong>No current or upcoming rounds available.</strong>
          <div style={{ marginTop: 8, color: "rgba(255,255,255,0.65)" }}>
            This can happen if the DB has no rounds for {gameType}, or all rounds are finished.
          </div>
        </div>
      )}

      {current.status === "error" && (
        <div style={{ padding: 12, border: "1px solid #a33", borderRadius: 10 }}>
          <strong>Error</strong>
          <div style={{ marginTop: 8 }}>{current.message}</div>
        </div>
      )}

      {current.status === "ok" && (
        <div style={{ display: "grid", gap: 12 }}>
          <div style={{ padding: 12, border: "1px solid #444", borderRadius: 10 }}>
            <div>
              <strong>Round:</strong> #{current.data.round.id}
            </div>
            <div>
              <strong>Status:</strong> {current.data.roundStatus}
            </div>
            <div>
              <strong>Start:</strong> {current.data.round.startDate}
            </div>
            <div>
              <strong>End:</strong> {current.data.round.endDate}
            </div>
          </div>

          {/* MODEL RUNS */}
          {modelRuns.status === "idle" && <div />}

          {modelRuns.status === "loading" && <div>Loading model runs…</div>}

          {modelRuns.status === "error" && (
            <div style={{ padding: 12, border: "1px solid #a33", borderRadius: 10 }}>
              <strong>Error</strong>
              <div style={{ marginTop: 8 }}>{modelRuns.message}</div>
            </div>
          )}

          {modelRuns.status === "ok" && !selectedRun && (
            <div style={{ padding: 12, border: "1px solid #444", borderRadius: 10 }}>
              <strong>No model runs found for this round.</strong>
              <div style={{ marginTop: 8, color: "rgba(255,255,255,0.65)" }}>
                Expected preset runs (32/64/128/256) to exist.
              </div>
            </div>
          )}

          {modelRuns.status === "ok" && selectedRun && (
            <div style={{ padding: 12, border: "1px solid #444", borderRadius: 10 }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                <div>
                  <strong>
                    Model picks ({selectedRun.budgetInSek} SEK, trigger {selectedRun.trigger})
                  </strong>
                </div>
                <div style={{ color: "rgba(255,255,255,0.65)" }}>
                  cost {selectedRun.totalCostInSek} • half {selectedRun.halfGuardsCount} • full{" "}
                  {selectedRun.fullGuardsCount}
                </div>
              </div>

              <div style={{ marginTop: 12, display: "grid", gap: 10 }}>
                {current.data.round.matches.map((m) => (
                  <div
                    key={m.matchNumber}
                    style={{
                      display: "grid",
                      gridTemplateColumns: "56px 1fr auto",
                      alignItems: "center",
                      gap: 12,
                      paddingBottom: 10,
                      borderBottom: "1px solid rgba(255,255,255,0.10)",
                    }}
                  >
                    <div style={{ fontWeight: 800 }}>{m.matchNumber}</div>

                    <div>
                      <div style={{ fontWeight: 700 }}>
                        {m.homeTeamName} – {m.awayTeamName}
                      </div>
                      <div style={{ color: "rgba(255,255,255,0.55)", fontSize: 12 }}>
                        {m.startDate}
                      </div>
                    </div>

                    {/* 1 X 2 boxes */}
                    <div style={{ display: "flex", gap: 10, justifySelf: "end" }}>
                      {(["HOME_WIN", "DRAW", "AWAY_WIN"] as const).map((o) => {
                        const selected = isSelected(
                          selectedRun.selections,
                          m.matchNumber,
                          o,
                        );

                        return (
                          <div
                            key={o}
                            title="1=Home, X=Draw, 2=Away"
                            style={{
                              width: 54,
                              height: 32,
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "center",
                              borderRadius: 6,
                              fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
                              fontWeight: 900,
                              border: selected
                                ? "1px solid rgba(255,255,255,0.10)"
                                : "1px solid rgba(255,255,255,0.35)",
                              background: selected ? "#d35400" : "transparent",
                              color: selected ? "#fff" : "rgba(255,255,255,0.85)",
                              userSelect: "none",
                            }}
                          >
                            {outcomeLabel(o)}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
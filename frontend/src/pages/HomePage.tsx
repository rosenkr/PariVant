import { useEffect, useMemo, useState } from "react";
import {
  Box,
  Chip,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Typography,
} from "@mui/material";

import { Page } from "../components/layout/Page";
import { useCurrentRound } from "../hooks/useCurrentRound";
import { useModelRuns } from "../hooks/useModelRuns";

import type { GameType } from "../types/round";
import type { Outcome } from "../types/modelRun";

/* 
useState: stores user selections (game type + budget).

useEffect: one-time sync from AUTO backend response → dropdown state.

useMemo: optional optimization for choosing which run to show.

setState in effect: safe here because it’s guarded and non-looping.
*/

const GAME_TYPES: GameType[] = ["STRYKTIPSET", "EUROPATIPSET", "TOPPTIPSET"];
const PRESET_BUDGETS = [32, 64, 128, 256] as const;

function isSelected(
  selected: Outcome[] | undefined,
  outcome: Outcome,
): boolean {
  return !!selected && selected.includes(outcome);
}

export default function HomePage() {
  // null => initial load uses /public/current (no param)
  const [gameType, setGameType] = useState<GameType | null>(null);
  const [budget, setBudget] = useState<(typeof PRESET_BUDGETS)[number]>(64);

  const currentQuery = useCurrentRound(gameType ?? undefined);

  // once we get the initial AUTO result, lock dropdown to that gameType
  useEffect(() => {
    if (gameType !== null) return;
    const r = currentQuery.data;
    if (r && r.kind === "ok") {
      setGameType(r.data.selectedGameType);
    }
  }, [gameType, currentQuery.data]);

  const current = currentQuery.data;
  const roundId =
    current && current.kind === "ok" ? current.data.round.id : null;

  const modelRunsQuery = useModelRuns(roundId);

  const selectedRun = useMemo(() => {
    const runsRes = modelRunsQuery.data;
    if (!runsRes || runsRes.kind !== "ok") return null;
    return runsRes.data.find((r) => r.budgetInSek === budget) ?? null;
  }, [modelRunsQuery.data, budget]);

  return (
    <Page maxWidth="lg">
      <Stack spacing={2}>
        <Typography variant="h4" fontWeight={800}>
          Betting Model (MVP)
        </Typography>

        {/* Controls */}
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Stack
            direction="row"
            spacing={2}
            alignItems="center"
            flexWrap="wrap"
          >
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel>Game</InputLabel>
              <Select
                label="Game"
                value={gameType ?? ""}
                onChange={(e) => setGameType(e.target.value as GameType)}
                displayEmpty
              >
                {/* if gameType is still null, we show empty until AUTO returns */}
                {GAME_TYPES.map((gt) => (
                  <MenuItem key={gt} value={gt}>
                    {gt}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel>Budget</InputLabel>
              <Select
                label="Budget"
                value={budget}
                onChange={(e) =>
                  setBudget(
                    Number(e.target.value) as (typeof PRESET_BUDGETS)[number],
                  )
                }
              >
                {PRESET_BUDGETS.map((b) => (
                  <MenuItem key={b} value={b}>
                    {b} SEK
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <Typography variant="body2" color="text.secondary">
              Public page reads existing runs (does not trigger runs).
            </Typography>
          </Stack>
        </Paper>

        {/* Current round */}
        {currentQuery.isLoading && (
          <Paper variant="outlined" sx={{ p: 2 }}>
            Loading current round…
          </Paper>
        )}

        {current && current.kind === "no-round" && (
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography fontWeight={700}>
              No current or upcoming rounds available.
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              This means the DB currently has no RUNNING/UPCOMING round for the
              selected game type.
            </Typography>
          </Paper>
        )}

        {current &&
          (current.kind === "server-error" ||
            current.kind === "network-error") && (
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography fontWeight={700}>Error</Typography>
              <Typography variant="body2" sx={{ mt: 1 }}>
                {current.kind === "server-error"
                  ? `${current.status}: ${current.message}`
                  : current.message}
              </Typography>
            </Paper>
          )}

        {current && current.kind === "ok" && (
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Stack spacing={0.5}>
              <Typography fontWeight={800}>
                Round: #{current.data.round.id}
              </Typography>
              <Typography variant="body2">
                Status: <b>{current.data.roundStatus}</b>
              </Typography>
              <Typography variant="body2">
                Start: <b>{current.data.round.startDate}</b>
              </Typography>
              <Typography variant="body2">
                End: <b>{current.data.round.endDate}</b>
              </Typography>
            </Stack>
          </Paper>
        )}

        {/* Model picks */}
        {current && current.kind === "ok" && (
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Stack spacing={1.5}>
              <Stack
                direction="row"
                alignItems="baseline"
                justifyContent="space-between"
                flexWrap="wrap"
                gap={1}
              >
                <Typography fontWeight={800}>
                  Model picks ({budget} SEK
                  {selectedRun ? `, trigger ${selectedRun.trigger}` : ""})
                </Typography>

                {selectedRun && (
                  <Typography variant="body2" color="text.secondary">
                    cost {selectedRun.totalCostInSek} • half{" "}
                    {selectedRun.halfGuardsCount} • full{" "}
                    {selectedRun.fullGuardsCount}
                  </Typography>
                )}
              </Stack>

              <Divider />

              {modelRunsQuery.isLoading && (
                <Typography variant="body2">Loading model runs…</Typography>
              )}

              {modelRunsQuery.data &&
                (modelRunsQuery.data.kind === "server-error" ||
                  modelRunsQuery.data.kind === "network-error") && (
                  <Typography variant="body2" color="error">
                    {modelRunsQuery.data.kind === "server-error"
                      ? `${modelRunsQuery.data.status}: ${modelRunsQuery.data.message}`
                      : modelRunsQuery.data.message}
                  </Typography>
                )}

              {modelRunsQuery.data?.kind === "ok" && !selectedRun && (
                <Typography variant="body2" color="text.secondary">
                  No model run found for {budget} SEK yet.
                </Typography>
              )}

              {selectedRun &&
                current.data.round.matches.map((m) => {
                  const key = String(m.matchNumber);
                  const sel = selectedRun.selections[key];

                  return (
                    <Box
                      key={m.matchNumber}
                      sx={{
                        display: "grid",
                        gridTemplateColumns: "48px 1fr auto",
                        alignItems: "center",
                        gap: 2,
                        py: 1,
                        borderBottom: "1px solid",
                        borderColor: "divider",
                      }}
                    >
                      <Typography variant="body2" color="text.secondary">
                        {m.matchNumber}
                      </Typography>

                      <Box>
                        <Typography fontWeight={700}>
                          {m.homeTeamName} – {m.awayTeamName}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {m.startDate}
                        </Typography>
                      </Box>

                      {/* 1 X 2 boxes */}
                      <Stack direction="row" spacing={1}>
                        <Chip
                          label="1"
                          clickable={false}
                          sx={{
                            width: 44,
                            borderRadius: 1,
                            fontWeight: 800,
                            bgcolor: isSelected(sel, "HOME_WIN")
                              ? "warning.main"
                              : "transparent",
                            color: isSelected(sel, "HOME_WIN")
                              ? "warning.contrastText"
                              : "text.primary",
                            border: "1px solid",
                            borderColor: "divider",
                          }}
                        />
                        <Chip
                          label="X"
                          clickable={false}
                          sx={{
                            width: 44,
                            borderRadius: 1,
                            fontWeight: 800,
                            bgcolor: isSelected(sel, "DRAW")
                              ? "warning.main"
                              : "transparent",
                            color: isSelected(sel, "DRAW")
                              ? "warning.contrastText"
                              : "text.primary",
                            border: "1px solid",
                            borderColor: "divider",
                          }}
                        />
                        <Chip
                          label="2"
                          clickable={false}
                          sx={{
                            width: 44,
                            borderRadius: 1,
                            fontWeight: 800,
                            bgcolor: isSelected(sel, "AWAY_WIN")
                              ? "warning.main"
                              : "transparent",
                            color: isSelected(sel, "AWAY_WIN")
                              ? "warning.contrastText"
                              : "text.primary",
                            border: "1px solid",
                            borderColor: "divider",
                          }}
                        />
                      </Stack>
                    </Box>
                  );
                })}
            </Stack>
          </Paper>
        )}
      </Stack>
    </Page>
  );
}

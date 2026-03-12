import { Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Paper, Stack, Typography } from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { Page } from "../components/layout/Page";
import { RoundHeader } from "../components/RoundHeader";
import { MatchRow } from "../components/MatchRow";

import { useCurrentRound } from "../hooks/useCurrentRound";
import { useModelRuns } from "../hooks/useModelRuns";

import type { GameType } from "../types/round";
import type { ModelRunView, Outcome } from "../types/modelRun";

const PRESET_BUDGETS = [32, 64, 128, 256] as const;

function parseSelections(run: ModelRunView): Record<string, Outcome[]> {
  if (run.selections) return run.selections;

  if (run.selectionsJson) {
    try {
      const obj = JSON.parse(run.selectionsJson) as unknown;
      if (obj && typeof obj === "object") return obj as Record<string, Outcome[]>;
    } catch {
      // ignore
    }
  }
  return {};
}

export default function HomePage() {
  const [gameType, setGameType] = useState<GameType>("STRYKTIPSET");
  const [budget, setBudget] = useState<(typeof PRESET_BUDGETS)[number]>(64);
  const [loginDialogOpen, setLoginDialogOpen] = useState(false);

  const currentQuery = useCurrentRound(gameType);
  const current = currentQuery.data;

  // If backend returns selectedGameType (auto fallback), sync dropdown once.
  useEffect(() => {
    if (!current || current.kind !== "ok") return;
    if (current.data.selectedGameType !== gameType) setGameType(current.data.selectedGameType);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current]);

  const roundId = current && current.kind === "ok" ? current.data.round.id : null;
  const modelRunsQuery = useModelRuns(roundId);

  const selectedRun = useMemo(() => {
    const runsRes = modelRunsQuery.data;
    if (!runsRes || runsRes.kind !== "ok") return null;
    return runsRes.data.find((r) => r.budgetInSek === budget) ?? null;
  }, [modelRunsQuery.data, budget]);

  const selectionsByMatch = useMemo(() => (selectedRun ? parseSelections(selectedRun) : {}), [selectedRun]);

  const headerInfo = useMemo(() => {
    if (!current || current.kind !== "ok") return {};
    return {
      roundId: current.data.round.id,
      roundStatus: current.data.roundStatus,
      start: current.data.round.startDate,
      end: current.data.round.endDate,
    };
  }, [current]);

  const onBoxClick = () => setLoginDialogOpen(true);

  return (
    <Page maxWidth="lg">
      <Stack spacing={2}>
        <RoundHeader
          gameType={gameType}
          setGameType={setGameType}
          budget={budget}
          setBudget={setBudget}
          roundId={headerInfo.roundId}
          roundStatus={headerInfo.roundStatus}
          start={headerInfo.start}
          end={headerInfo.end}
        />

        <Paper
          sx={{
            overflow: "hidden",
            backgroundColor: "rgba(255,255,255,0.04)",
            borderColor: "rgba(255,45,142,0.25)",
          }}
        >
          {currentQuery.isLoading && (
            <Box sx={{ p: 2 }}>
              <Typography>Loading current round…</Typography>
            </Box>
          )}

          {!currentQuery.isLoading && current?.kind === "no-round" && (
            <Box sx={{ p: 2 }}>
              <Typography color="text.secondary">No current or upcoming rounds available.</Typography>
            </Box>
          )}

          {!currentQuery.isLoading && (current?.kind === "server-error" || current?.kind === "network-error") && (
            <Box sx={{ p: 2 }}>
              <Typography color="error">
                {current.kind === "server-error"
                  ? `Server error ${current.status}: ${current.message}`
                  : `Network error: ${current.message}`}
              </Typography>
            </Box>
          )}

          {current && current.kind === "ok" && selectedRun && (
            <Box>
              <Box
                sx={{
                  px: 2,
                  py: 1.0,
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  borderBottom: "1px solid rgba(255,255,255,0.08)",
                }}
              >
                <Typography sx={{ fontWeight: 800 }}>
                  Model picks ({budget} SEK, trigger {selectedRun.trigger})
                </Typography>

                <Typography variant="body2" sx={{ opacity: 0.75 }}>
                  cost {selectedRun.totalCostInSek} • half {selectedRun.halfGuardsCount} • full {selectedRun.fullGuardsCount}
                </Typography>
              </Box>

              {current.data.round.matches.map((m, idx) => {
                const key = String(m.matchNumber);
                const sel = selectionsByMatch[key] ?? [];

                return (
                  <Box
                    key={m.matchNumber}
                    sx={{
                      backgroundColor: idx % 2 === 0 ? "rgba(255,255,255,0.02)" : "rgba(0,0,0,0.10)",
                    }}
                  >
                    <MatchRow
                      index={m.matchNumber}
                      home={m.homeTeamName}
                      away={m.awayTeamName}
                      kickoff={m.startDate}
                      selected={sel}
                      market={m.market}
                      publicPick={m.publicPick}
                      onSelectionClick={onBoxClick}
                    />
                  </Box>
                );
              })}
            </Box>
          )}

          {current && current.kind === "ok" && !selectedRun && (
            <Box sx={{ p: 2 }}>
              <Typography color="text.secondary">
                No model run found for budget {budget} SEK yet.
              </Typography>
            </Box>
          )}
        </Paper>

        <Dialog open={loginDialogOpen} onClose={() => setLoginDialogOpen(false)}>
          <DialogTitle>Log in required</DialogTitle>
          <DialogContent>
            <Typography>
              Log in to make and save your own selections.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setLoginDialogOpen(false)}>Close</Button>
          </DialogActions>
        </Dialog>
      </Stack>
    </Page>
  );
}
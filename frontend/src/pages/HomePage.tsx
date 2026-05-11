import { Box, Paper, Stack, Typography } from "@mui/material";
import { useEffect, useMemo, useRef, useState } from "react";
import { Page } from "../components/layout/Page";
import { RoundHeader } from "../components/RoundHeader";
import { RoundStatusTabs } from "../components/RoundStatusTabs";
import { MatchRow } from "../components/MatchRow";
import { MatchDetailsPanel } from "../components/MatchDetailsPanel";
import { RoundSelectorTabs } from "../components/RoundSelectorTabs";
import { useRoundsByFilters } from "../hooks/useRoundsByFilters";
import { useModelRuns } from "../hooks/useModelRuns";
import { useLiveRound } from "../hooks/useLiveRound";
import { useRoundProviderPredictions } from "../hooks/useRoundProviderPredictions";

import type { LiveMatchUpdate } from "../types/liveScore";
import type {
  ModelRunView,
  Outcome,
  ProbabilityTripleDtoShape,
} from "../types/modelRun";
import type {
  MatchProviderPredictionsView,
  ProviderPredictionView,
} from "../types/providerPrediction";
import type { RoundStatus, RoundType, RoundView } from "../types/round";

type BudgetValue = 32 | 64 | 128 | 256;


function parseSelections(run: ModelRunView): Record<string, Outcome[]> {
  if (run.selections) return run.selections;

  if (run.selectionsJson) {
    try {
      const obj = JSON.parse(run.selectionsJson) as unknown;
      if (obj && typeof obj === "object") {
        return obj as Record<string, Outcome[]>;
      }
    } catch {
      // ignore
    }
  }

  return {};
}

function parseBasePicks(run: ModelRunView | null): Record<string, Outcome> {
  if (!run) return {};

  if (run.basePicks) return run.basePicks;

  if (run.basePicksJson) {
    try {
      const obj = JSON.parse(run.basePicksJson) as unknown;
      if (obj && typeof obj === "object") {
        return obj as Record<string, Outcome>;
      }
    } catch {
      // ignore
    }
  }

  return {};
}

function parseInternalProbabilities(
  run: ModelRunView | null,
): Record<string, ProbabilityTripleDtoShape> {
  if (!run) return {};

  if (run.internalProbabilities) return run.internalProbabilities;

  if (run.internalProbabilitiesJson) {
    try {
      const obj = JSON.parse(run.internalProbabilitiesJson) as unknown;
      if (obj && typeof obj === "object") {
        return obj as Record<string, ProbabilityTripleDtoShape>;
      }
    } catch {
      // ignore
    }
  }

  return {};
}

function sortRoundsForStatus(
  rounds: RoundView[],
  status: RoundStatus,
): RoundView[] {
  const copy = [...rounds];

  if (status === "ENDED") {
    return copy.sort(
      (a, b) =>
        new Date(b.startTime).getTime() - new Date(a.startTime).getTime(),
    );
  }

  return copy.sort(
    (a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
  );
}

function statusUiLabel(status: RoundStatus): string {
  switch (status) {
    case "UPCOMING":
      return "upcoming";
    case "RUNNING":
      return "live";
    case "ENDED":
      return "ended";
  }
}

function findProvidersForMatch(
  providerMatches: MatchProviderPredictionsView[] | undefined,
  matchNumber: number,
): ProviderPredictionView[] {
  if (!providerMatches) return [];
  return (
    providerMatches.find((m) => m.matchNumber === matchNumber)?.providers ?? []
  );
}

function groupRoundsByType(
  rounds: RoundView[],
): Record<RoundType, RoundView[]> {
  return {
    STRYKTIPSET: rounds.filter((r) => r.roundType === "STRYKTIPSET"),
    EUROPATIPSET: rounds.filter((r) => r.roundType === "EUROPATIPSET"),
    TOPPTIPSET: rounds.filter((r) => r.roundType === "TOPPTIPSET"),
  };
}

function pickDefaultRoundType(rounds: RoundView[]): RoundType {
  const byType = groupRoundsByType(rounds);

  if (byType.STRYKTIPSET.length > 0) return "STRYKTIPSET";
  if (byType.EUROPATIPSET.length > 0) return "EUROPATIPSET";
  if (byType.TOPPTIPSET.length > 0) return "TOPPTIPSET";
  return "STRYKTIPSET";
}

export default function HomePage() {
  const [selectedStatus, setSelectedStatus] = useState<RoundStatus>("UPCOMING");
  const [roundType, setRoundType] = useState<RoundType>("STRYKTIPSET");
  const [budget, setBudget] = useState<BudgetValue>(64);

  const [selectedMatchNumber, setSelectedMatchNumber] = useState<number | null>(
    null,
  );
  const [selectedRoundId, setSelectedRoundId] = useState<number | null>(null);
  const autoPickedTypeByStatusRef = useRef<Record<RoundStatus, boolean>>({
    UPCOMING: false,
    RUNNING: false,
    ENDED: false,
  });

  const roundsQuery = useRoundsByFilters(selectedStatus);
  const roundsResult = roundsQuery.data;

  const roundsForSelectedType = useMemo(() => {
    if (!roundsResult || roundsResult.kind !== "ok") return [];
    return roundsResult.data.filter((r) => r.roundType === roundType);
  }, [roundsResult, roundType]);

  const sortedRounds = useMemo(() => {
    return sortRoundsForStatus(roundsForSelectedType, selectedStatus);
  }, [roundsForSelectedType, selectedStatus]);

  const activeRound = useMemo(() => {
    if (sortedRounds.length === 0) return null;

    if (selectedRoundId != null) {
      const selected = sortedRounds.find((r) => r.id === selectedRoundId);
      if (selected) return selected;
    }

    return sortedRounds[0] ?? null;
  }, [sortedRounds, selectedRoundId]);

  useEffect(() => {
    autoPickedTypeByStatusRef.current[selectedStatus] = false;
    setSelectedRoundId(null);
    setSelectedMatchNumber(null);
  }, [selectedStatus]);

  useEffect(() => {
    if (!roundsResult || roundsResult.kind !== "ok") return;
    if (autoPickedTypeByStatusRef.current[selectedStatus]) return;

    const preferred = pickDefaultRoundType(roundsResult.data);
    setRoundType(preferred);
    autoPickedTypeByStatusRef.current[selectedStatus] = true;
  }, [roundsResult, selectedStatus]);

  useEffect(() => {
    if (sortedRounds.length === 0) {
      setSelectedRoundId(null);
      return;
    }

    setSelectedRoundId((current) => {
      if (current != null && sortedRounds.some((r) => r.id === current)) {
        return current;
      }
      return sortedRounds[0].id;
    });
  }, [sortedRounds]);

  const roundId = activeRound?.id ?? null;
  const roundStatus = activeRound ? selectedStatus : null;

  const modelRunsQuery = useModelRuns(roundId);
  const providerPredictionsQuery = useRoundProviderPredictions(roundId);

  const selectedRun = useMemo(() => {
    const runsRes = modelRunsQuery.data;
    if (!runsRes || runsRes.kind !== "ok") return null;
    return runsRes.data.find((r) => r.budgetInSek === budget) ?? null;
  }, [modelRunsQuery.data, budget]);

  const selectionsByMatch = useMemo(
    () => (selectedRun ? parseSelections(selectedRun) : {}),
    [selectedRun],
  );

  const basePicksByMatch = useMemo(
    () => parseBasePicks(selectedRun),
    [selectedRun],
  );

  const internalProbabilitiesByMatch = useMemo(
    () => parseInternalProbabilities(selectedRun),
    [selectedRun],
  );

  useEffect(() => {
    if (!activeRound || activeRound.matches.length === 0) {
      setSelectedMatchNumber(null);
      return;
    }

    setSelectedMatchNumber((current) => {
      if (
        current != null &&
        activeRound.matches.some((m) => m.matchNumber === current)
      ) {
        return current;
      }
      return activeRound.matches[0]?.matchNumber ?? null;
    });
  }, [activeRound]);

  const selectedMatch = useMemo(() => {
    if (!activeRound || selectedMatchNumber == null) return null;
    return (
      activeRound.matches.find((m) => m.matchNumber === selectedMatchNumber) ??
      null
    );
  }, [activeRound, selectedMatchNumber]);

  const providerPredictionResult = providerPredictionsQuery.data;
  const providerMatches = useMemo(() => {
    if (!providerPredictionResult || providerPredictionResult.kind !== "ok") {
      return [];
    }
    return providerPredictionResult.data.matches;
  }, [providerPredictionResult]);

  const selectedMatchProviders = useMemo(() => {
    if (!selectedMatch) return [];
    return findProvidersForMatch(providerMatches, selectedMatch.matchNumber);
  }, [providerMatches, selectedMatch]);

  const selectedInternal = useMemo(() => {
    if (!selectedMatch) return null;
    return (
      internalProbabilitiesByMatch[String(selectedMatch.matchNumber)] ?? null
    );
  }, [internalProbabilitiesByMatch, selectedMatch]);

  const isRunning = roundStatus === "RUNNING";
  const enableLive = roundId != null && roundStatus === "RUNNING";
  const live = useLiveRound(roundId, enableLive);

  const liveByMatchNumber = useMemo(() => {
    const map = new Map<number, LiveMatchUpdate>();
    if (!live.snapshot) return map;
    for (const m of live.snapshot.matches) {
      map.set(m.matchNumber, m);
    }
    return map;
  }, [live.snapshot]);

  const emptyMessage = useMemo(() => {
    const statusText = statusUiLabel(selectedStatus);
    return `No ${statusText} ${roundType.toLowerCase()} round available right now.`;
  }, [selectedStatus, roundType]);

  return (
    <Page maxWidth="xl">
      <Stack spacing={2}>
        <Paper
          sx={(theme) => ({
            backgroundColor: theme.appColors.accent.soft,
            borderColor: theme.appColors.border.muted,
            overflow: "hidden",
          })}
        >
          <RoundStatusTabs
            value={selectedStatus}
            onChange={setSelectedStatus}
          />
        </Paper>

        <RoundHeader
          roundType={roundType}
          setRoundType={setRoundType}
          budget={budget}
          setBudget={setBudget}
          roundId={activeRound?.id}
          roundStatus={activeRound ? selectedStatus : undefined}
          start={activeRound?.startTime}
        />

        {sortedRounds.length > 1 && (
          <RoundSelectorTabs
            rounds={sortedRounds}
            selectedRoundId={activeRound?.id ?? null}
            onSelectRound={setSelectedRoundId}
          />
        )}

        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1.45fr) 420px" },
            gap: 2,
            alignItems: "start",
          }}
        >
          <Paper
            sx={(theme) => ({
              overflow: "hidden",
              backgroundColor: theme.appColors.accent.soft,
              borderColor: isRunning
                ? theme.appColors.live.border
                : theme.appColors.border.muted,
              ...(isRunning
                ? {
                    animation: "livePanelPulse 7.5s ease-in-out infinite",
                    "@keyframes livePanelPulse": {
                      "0%": {
                        boxShadow: `0 0 0 1px ${theme.appColors.live.soft}, 0 0 24px ${theme.appColors.live.glow}`,
                      },
                      "50%": {
                        boxShadow: `0 0 0 1px ${theme.appColors.live.border}, 0 0 36px ${theme.appColors.live.glow}`,
                      },
                      "100%": {
                        boxShadow: `0 0 0 1px ${theme.appColors.live.soft}, 0 0 24px ${theme.appColors.live.glow}`,
                      },
                    },
                  }
                : null),
            })}
          >
            {roundsQuery.isLoading && (
              <Box sx={{ p: 2 }}>
                <Typography>Loading rounds…</Typography>
              </Box>
            )}

            {!roundsQuery.isLoading &&
              roundsResult?.kind === "ok" &&
              !activeRound && (
                <Box sx={{ p: 2 }}>
                  <Typography color="text.secondary">{emptyMessage}</Typography>
                </Box>
              )}

            {!roundsQuery.isLoading &&
              (roundsResult?.kind === "server-error" ||
                roundsResult?.kind === "network-error") && (
                <Box sx={{ p: 2 }}>
                  <Typography color="error">
                    {roundsResult.kind === "server-error"
                      ? `Server error ${roundsResult.status}: ${roundsResult.message}`
                      : `Network error: ${roundsResult.message}`}
                  </Typography>
                </Box>
              )}

            {activeRound && selectedRun && (
              <Box>
                <Box
                  sx={(theme) => ({
                    px: 2,
                    py: 1,
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    borderBottom: `1px solid ${theme.appColors.border.subtle}`,
                  })}
                >
                  <Typography sx={{ fontWeight: 800 }}>
                    Model picks ({budget} SEK, trigger {selectedRun.trigger})
                  </Typography>

                  <Typography variant="body2" sx={{ opacity: 0.75 }}>
                    cost {selectedRun.totalCostInSek} • half{" "}
                    {selectedRun.halfGuardsCount} • full{" "}
                    {selectedRun.fullGuardsCount ?? 0}
                    {enableLive && (
                      <>
                        {" "}
                        •{" "}
                        <span style={{ opacity: 0.9 }}>
                          LIVE{" "}
                          {live.state.status === "live"
                            ? "connected"
                            : "connecting"}
                        </span>
                      </>
                    )}
                  </Typography>
                </Box>

                {activeRound.matches.map((m, idx) => {
                  const key = String(m.matchNumber);
                  const sel = selectionsByMatch[key] ?? [];
                  const basePick = basePicksByMatch[key] ?? null;
                  const liveUpdate =
                    liveByMatchNumber.get(m.matchNumber) ?? null;

                  return (
                    <Box
                      key={m.matchNumber}
                      sx={(theme) => ({
                        backgroundColor:
                          idx % 2 === 0
                            ? theme.appColors.accent.soft
                            : theme.palette.mode === "dark"
                              ? "rgba(0,0,0,0.10)"
                              : "rgba(0,0,0,0.03)",
                      })}
                    >
                      <MatchRow
                        index={m.matchNumber}
                        home={m.homeTeamName}
                        away={m.awayTeamName}
                        kickoff={m.startTime}
                        selected={sel}
                        basePick={basePick}
                        live={liveUpdate}
                        isActive={selectedMatchNumber === m.matchNumber}
                        onClick={() => setSelectedMatchNumber(m.matchNumber)}
                        marketFallbackUsed={m.marketFallbackUsed}
                        marketFallbackReason={m.marketFallbackReason}
                        showSelectionDisabledHintOnHover
                        selectionDisabledHintText="Selections can’t be changed from the home page."
                      />
                    </Box>
                  );
                })}
              </Box>
            )}

            {activeRound && !selectedRun && (
              <Box sx={{ p: 2 }}>
                <Typography color="text.secondary">
                  No model run found for budget {budget} SEK yet.
                </Typography>
              </Box>
            )}
          </Paper>

          <MatchDetailsPanel
            match={selectedMatch}
            publicPick={selectedMatch?.publicPick ?? null}
            market={selectedMatch?.market ?? null}
            providers={selectedMatchProviders}
            internal={selectedInternal}
            providerQueryState={providerPredictionsQuery}
          />
        </Box>
      </Stack>
    </Page>
  );
}

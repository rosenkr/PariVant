import AddRoundedIcon from "@mui/icons-material/AddRounded";
import SettingsBackupRestoreRoundedIcon from "@mui/icons-material/SettingsBackupRestoreRounded";
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Paper,
  Slider,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { createCoupon, getCoupon, getCoupons } from "../api/private/coupons";
import { runModelSelection } from "../api/private/modelSelection";
import { useAuth } from "../auth/AuthContext";
import { Page } from "../components/layout/Page";
import { SelectionBox } from "../components/SelectionBox";
import { useModelRuns } from "../hooks/useModelRuns";
import { useRoundProviderPredictions } from "../hooks/useRoundProviderPredictions";
import { useRoundsByFilters } from "../hooks/useRoundsByFilters";
import type { CouponRequest, CouponResponse, CouponSelections } from "../types/coupon";
import type { ModelRunView, Outcome } from "../types/modelRun";
import type { ModelSelectionRequest } from "../types/modelSelection";
import type { ProbabilityTriple } from "../types/probabilityTriple";
import type { MatchProviderPredictionsView } from "../types/providerPrediction";
import type { MatchView, RoundType, RoundView } from "../types/round";
import { formatRoundDateTime, formatTimeOnly } from "../utils/time";

const OUTCOMES: Array<{ outcome: Outcome; label: string }> = [
  { outcome: "HOME_WIN", label: "1" },
  { outcome: "DRAW", label: "X" },
  { outcome: "AWAY_WIN", label: "2" },
];

const DEFAULT_BUDGET_IN_SEK = 64;
const DEFAULT_USER_PROBABILITY: ProbabilityTriple = {
  homeWin: 0.33,
  draw: 0.34,
  awayWin: 0.33,
};

function roundTypeLabel(roundType: RoundType): string {
  switch (roundType) {
    case "STRYKTIPSET":
      return "Stryktipset";
    case "EUROPATIPSET":
      return "Europatipset";
    case "TOPPTIPSET":
      return "Topptipset";
  }
}

function roundListLabel(round: RoundView): string {
  const date = formatRoundDateTime(round.startTime)?.replace(" - ", " ");
  return `${roundTypeLabel(round.roundType)} - ${date ?? round.startTime}`;
}

function initialSelections(round: RoundView): Record<number, Outcome[]> {
  const selections: Record<number, Outcome[]> = {};
  for (const match of round.matches) {
    selections[match.matchNumber] = [];
  }
  return selections;
}

function initialUserProbabilities(round: RoundView): Record<number, ProbabilityTriple> {
  const probabilities: Record<number, ProbabilityTriple> = {};
  for (const match of round.matches) {
    probabilities[match.matchNumber] = DEFAULT_USER_PROBABILITY;
  }
  return probabilities;
}

function toCouponSelections(
  selectionsByMatch: Record<number, Outcome[]>,
): CouponSelections {
  const out: CouponSelections = {};
  for (const [matchNumber, outcomes] of Object.entries(selectionsByMatch)) {
    out[matchNumber] = outcomes;
  }
  return out;
}

function selectionIsComplete(round: RoundView, selections: Record<number, Outcome[]>) {
  return round.matches.every((match) => selections[match.matchNumber]?.length > 0);
}

function computeDraftPrice(
  round: RoundView | null,
  selections: Record<number, Outcome[]>,
): number | null {
  if (!round || !selectionIsComplete(round, selections)) return null;

  return round.matches.reduce((price, match) => {
    return price * (selections[match.matchNumber]?.length ?? 0);
  }, 1);
}

function couponSelectionLabels(selections: CouponSelections): string {
  return Object.entries(selections)
    .sort(([a], [b]) => Number(a) - Number(b))
    .map(([matchNumber, outcomes]) => {
      const labels = OUTCOMES
        .filter(({ outcome }) => outcomes.includes(outcome))
        .map(({ label }) => label)
        .join("");
      return `${matchNumber}: ${labels}`;
    })
    .join("  ");
}

function parseInternalProbabilities(
  run: ModelRunView | null,
): Record<string, ProbabilityTriple> {
  if (!run) return {};

  if (run.internalProbabilities) return run.internalProbabilities;

  if (run.internalProbabilitiesJson) {
    try {
      const parsed = JSON.parse(run.internalProbabilitiesJson) as unknown;
      if (parsed && typeof parsed === "object") {
        return parsed as Record<string, ProbabilityTriple>;
      }
    } catch {
      // ignore malformed model-run payloads and render N/A instead
    }
  }

  return {};
}

function probabilityForOutcome(
  probability: ProbabilityTriple | null | undefined,
  outcome: Outcome,
): number | null {
  if (!probability) return null;

  switch (outcome) {
    case "HOME_WIN":
      return probability.homeWin;
    case "DRAW":
      return probability.draw;
    case "AWAY_WIN":
      return probability.awayWin;
  }
}

function pct(value: number | null): string {
  if (value == null) return "N/A";
  return `${Math.round(value * 100)}%`;
}

function toSliderValue(probability: ProbabilityTriple): [number, number] {
  return [
    Math.round(probability.homeWin * 100),
    Math.round((probability.homeWin + probability.draw) * 100),
  ];
}

function fromSliderValue(value: number | number[]): ProbabilityTriple {
  const [firstRaw, secondRaw] = Array.isArray(value) ? value : [value, value];
  const first = Math.min(firstRaw, secondRaw);
  const second = Math.max(firstRaw, secondRaw);

  return {
    homeWin: first / 100,
    draw: (second - first) / 100,
    awayWin: (100 - second) / 100,
  };
}

function selectionsFromModelSelection(
  selections: Record<string, Outcome[]>,
): Record<number, Outcome[]> {
  const next: Record<number, Outcome[]> = {};
  for (const [matchNumber, outcomes] of Object.entries(selections)) {
    next[Number(matchNumber)] = OUTCOMES
      .map((option) => option.outcome)
      .filter((outcome) => outcomes.includes(outcome));
  }
  return next;
}

function providerMapByMatch(
  matches: MatchProviderPredictionsView[] | undefined,
): Map<number, MatchProviderPredictionsView> {
  return new Map((matches ?? []).map((match) => [match.matchNumber, match]));
}

function providerProbabilitiesForMatch(
  providerPredictionsByMatch: Map<number, MatchProviderPredictionsView>,
  matchNumber: number,
): ProbabilityTriple[] {
  const providerMatch = providerPredictionsByMatch.get(matchNumber);
  if (!providerMatch) return [];

  return providerMatch.providers
    .filter((provider) => {
      return (
        provider.status === "OK" &&
        provider.probabilityHome != null &&
        provider.probabilityDraw != null &&
        provider.probabilityAway != null
      );
    })
    .map((provider) => ({
      homeWin: provider.probabilityHome as number,
      draw: provider.probabilityDraw as number,
      awayWin: provider.probabilityAway as number,
    }));
}

function buildModelSelectionRequest(
  round: RoundView,
  userProbabilities: Record<number, ProbabilityTriple>,
  providerPredictionsByMatch: Map<number, MatchProviderPredictionsView>,
): ModelSelectionRequest {
  const contexts: ModelSelectionRequest["contexts"] = {};

  for (const match of round.matches) {
    if (!match.market || !match.publicPick) {
      throw new Error(`Missing market/public probabilities for match ${match.matchNumber}`);
    }

    const userProbability = userProbabilities[match.matchNumber];
    if (!userProbability) {
      throw new Error(`Missing user probabilities for match ${match.matchNumber}`);
    }

    contexts[match.matchNumber] = {
      market: match.market,
      publicPick: match.publicPick,
      providers: [
        ...providerProbabilitiesForMatch(providerPredictionsByMatch, match.matchNumber),
        userProbability,
      ],
    };
  }

  return {
    roundType: round.roundType,
    roundStartDate: round.startTime,
    matches: round.matches.map((match) => ({
      matchNumber: match.matchNumber,
      startDate: match.startTime,
      homeTeamName: match.homeTeamName,
      awayTeamName: match.awayTeamName,
    })),
    budgetInSek: DEFAULT_BUDGET_IN_SEK,
    contexts,
  };
}

function ProbabilityRow({
  label,
  probabilities,
}: {
  label: string;
  probabilities: ProbabilityTriple | null | undefined;
}) {
  return (
    <Box
      sx={{
        display: "grid",
        gridTemplateColumns: "72px repeat(3, 44px)",
        gap: 0.75,
        alignItems: "center",
      }}
    >
      <Typography variant="caption" sx={{ fontWeight: 800, opacity: 0.72 }}>
        {label}
      </Typography>
      {OUTCOMES.map(({ outcome, label: outcomeLabel }) => (
        <Typography key={outcome} variant="caption" sx={{ opacity: 0.72 }}>
          {outcomeLabel} {pct(probabilityForOutcome(probabilities, outcome))}
        </Typography>
      ))}
    </Box>
  );
}

function CouponDetail({ coupon }: { coupon: CouponResponse }) {
  return (
    <Paper
      sx={(theme) => ({
        mt: 2,
        p: 2,
        backgroundColor: theme.appColors.accent.soft,
        borderColor: theme.appColors.border.muted,
      })}
    >
      <Stack spacing={1}>
        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={1}
          justifyContent="space-between"
        >
          <Box>
            <Typography sx={{ fontWeight: 900 }}>
              {roundTypeLabel(coupon.roundType)} - Round #{coupon.roundId}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Coupon #{coupon.id} - Created {formatRoundDateTime(coupon.createdAt)}
            </Typography>
          </Box>
          <Typography sx={{ fontWeight: 900 }}>{coupon.status}</Typography>
        </Stack>

        <Typography>
          Price: <strong>{coupon.totalCost}</strong>
        </Typography>

        {coupon.correctPickCount != null && (
          <Typography>
            Correct picks: <strong>{coupon.correctPickCount}</strong>
          </Typography>
        )}

        <Typography variant="body2" color="text.secondary">
          {couponSelectionLabels(coupon.selections)}
        </Typography>
      </Stack>
    </Paper>
  );
}

function CouponMatchRow({
  match,
  internal,
  userProbability,
  selected,
  onToggle,
  onProbabilityChange,
  onProbabilityReset,
}: {
  match: MatchView;
  internal: ProbabilityTriple | null | undefined;
  userProbability: ProbabilityTriple;
  selected: Outcome[];
  onToggle: (outcome: Outcome) => void;
  onProbabilityChange: (probability: ProbabilityTriple) => void;
  onProbabilityReset: () => void;
}) {
  return (
    <Box
      sx={(theme) => ({
        px: 2,
        py: 1.1,
        display: "grid",
        gridTemplateColumns: "28px 1fr auto",
        gap: 1.5,
        alignItems: "center",
        borderBottom: `1px solid ${theme.appColors.border.subtle}`,
      })}
    >
      <Typography sx={{ fontWeight: 800, opacity: 0.9 }}>
        {match.matchNumber}
      </Typography>

      <Box>
        <Typography sx={{ fontWeight: 900 }}>
          {match.homeTeamName} <span style={{ opacity: 0.8 }}>-</span>{" "}
          {match.awayTeamName}
        </Typography>
        <Typography variant="caption" sx={{ opacity: 0.7 }}>
          {formatTimeOnly(match.startTime) ?? match.startTime}
        </Typography>
        <Stack spacing={0.25} sx={{ mt: 0.75 }}>
          <ProbabilityRow label="Public" probabilities={match.publicPick} />
          <ProbabilityRow label="Yours" probabilities={userProbability} />
          <ProbabilityRow label="Internal" probabilities={internal} />
        </Stack>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 1 }}>
          <Slider
            value={toSliderValue(userProbability)}
            min={0}
            max={100}
            step={1}
            disableSwap
            onChange={(_, value) => onProbabilityChange(fromSliderValue(value))}
            valueLabelDisplay="auto"
            sx={{ maxWidth: 280 }}
          />
          <Tooltip title="Reset your estimate">
            <IconButton size="small" onClick={onProbabilityReset}>
              <SettingsBackupRestoreRoundedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      </Box>

      <Stack direction="row" spacing={1}>
        {OUTCOMES.map(({ outcome, label }) => (
          <SelectionBox
            key={outcome}
            label={label}
            tone={selected.includes(outcome) ? "base" : "none"}
            onClick={() => onToggle(outcome)}
          />
        ))}
      </Stack>
    </Box>
  );
}

export function DashboardPage() {
  const { isAuthenticated, token } = useAuth();
  const queryClient = useQueryClient();
  const [creatorOpen, setCreatorOpen] = useState(false);
  const [selectedRound, setSelectedRound] = useState<RoundView | null>(null);
  const [selections, setSelections] = useState<Record<number, Outcome[]>>({});
  const [userProbabilities, setUserProbabilities] = useState<Record<number, ProbabilityTriple>>({});
  const [valuePickInternalProbabilities, setValuePickInternalProbabilities] = useState<Record<string, ProbabilityTriple> | null>(null);
  const [selectedCouponId, setSelectedCouponId] = useState<number | null>(null);

  const couponsQuery = useQuery({
    queryKey: ["coupons"],
    queryFn: () => getCoupons(token as string),
    enabled: isAuthenticated && token != null,
  });

  const selectedCouponQuery = useQuery({
    queryKey: ["coupon", selectedCouponId],
    queryFn: () => getCoupon(token as string, selectedCouponId as number),
    enabled: isAuthenticated && token != null && selectedCouponId != null,
  });

  const roundsQuery = useRoundsByFilters("UPCOMING");
  const modelRunsQuery = useModelRuns(selectedRound?.id ?? null);
  const providerPredictionsQuery = useRoundProviderPredictions(selectedRound?.id ?? null);
  const upcomingRounds = useMemo(() => {
    const result = roundsQuery.data;
    if (!result || result.kind !== "ok") return [];
    return [...result.data].sort(
      (a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
    );
  }, [roundsQuery.data]);

  const couponRoundIds = useMemo(() => {
    return new Set((couponsQuery.data ?? []).map((coupon) => coupon.roundId));
  }, [couponsQuery.data]);

  const createMutation = useMutation({
    mutationFn: (payload: CouponRequest) => {
      if (!token) throw new Error("Authentication required");
      return createCoupon(token, payload);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["coupons"] });
      await queryClient.invalidateQueries({ queryKey: ["coupon"] });
      setSelectedRound(null);
      setSelections({});
      setCreatorOpen(false);
    },
  });

  const modelSelectionMutation = useMutation({
    mutationFn: (payload: ModelSelectionRequest) => {
      if (!token) throw new Error("Authentication required");
      return runModelSelection(token, payload);
    },
    onSuccess: (result) => {
      setSelections(selectionsFromModelSelection(result.selections));
      setValuePickInternalProbabilities(result.internalProbabilities);
    },
  });

  const selectedModelRun = useMemo(() => {
    const result = modelRunsQuery.data;
    if (!result || result.kind !== "ok") return null;
    return result.data[0] ?? null;
  }, [modelRunsQuery.data]);
  const internalProbabilitiesByMatch = useMemo(
    () => valuePickInternalProbabilities ?? parseInternalProbabilities(selectedModelRun),
    [selectedModelRun, valuePickInternalProbabilities],
  );
  const providerPredictionsByMatch = useMemo(() => {
    const result = providerPredictionsQuery.data;
    return providerMapByMatch(result?.kind === "ok" ? result.data.matches : undefined);
  }, [providerPredictionsQuery.data]);
  const providerPredictionsAvailable =
    providerPredictionsQuery.data == null ||
    providerPredictionsQuery.data.kind === "ok";

  if (!isAuthenticated) {
    return null;
  }

  const canSubmit =
    selectedRound != null &&
    selectionIsComplete(selectedRound, selections) &&
    !couponRoundIds.has(selectedRound.id) &&
    !createMutation.isPending;
  const draftPrice = computeDraftPrice(selectedRound, selections);
  const canGetValuePicks =
    selectedRound != null &&
    !modelSelectionMutation.isPending &&
    !providerPredictionsQuery.isLoading &&
    providerPredictionsAvailable &&
    selectedRound.matches.every((match) => userProbabilities[match.matchNumber] != null);

  function selectRound(round: RoundView) {
    setSelectedRound(round);
    setSelections(initialSelections(round));
    setUserProbabilities(initialUserProbabilities(round));
    setValuePickInternalProbabilities(null);
    createMutation.reset();
    modelSelectionMutation.reset();
  }

  function toggleOutcome(matchNumber: number, outcome: Outcome) {
    setSelections((current) => {
      const existing = current[matchNumber] ?? [];
      const next = existing.includes(outcome)
        ? existing.filter((item) => item !== outcome)
        : [...existing, outcome];

      return {
        ...current,
        [matchNumber]: OUTCOMES
          .map((option) => option.outcome)
          .filter((option) => next.includes(option)),
      };
    });
  }

  function submitCoupon() {
    if (!selectedRound) return;

    createMutation.mutate({
      roundId: selectedRound.id,
      selections: toCouponSelections(selections),
    });
  }

  function updateUserProbability(matchNumber: number, probability: ProbabilityTriple) {
    setUserProbabilities((current) => ({
      ...current,
      [matchNumber]: probability,
    }));
    setValuePickInternalProbabilities(null);
    modelSelectionMutation.reset();
  }

  function resetUserProbability(matchNumber: number) {
    updateUserProbability(matchNumber, DEFAULT_USER_PROBABILITY);
  }

  function getValueSelections() {
    if (!selectedRound) return;

    modelSelectionMutation.mutate(
      buildModelSelectionRequest(
        selectedRound,
        userProbabilities,
        providerPredictionsByMatch,
      ),
    );
  }

  return (
    <Page maxWidth="lg">
      <Stack spacing={3} sx={{ px: { xs: 2, md: 0 }, py: { xs: 2, md: 4 } }}>
        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={2}
          alignItems={{ xs: "stretch", sm: "center" }}
          justifyContent="space-between"
        >
          <Box>
            <Typography variant="h4" sx={{ fontWeight: 900 }}>
              Dashboard
            </Typography>
            <Typography color="text.secondary">
              Create test coupons and view your saved coupons.
            </Typography>
          </Box>

          <Button
            variant="contained"
            startIcon={<AddRoundedIcon />}
            onClick={() => setCreatorOpen((open) => !open)}
          >
            Create coupon
          </Button>
        </Stack>

        <Paper sx={{ p: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: 900, mb: 1 }}>
            My coupons
          </Typography>

          {couponsQuery.isLoading && <Typography>Loading coupons...</Typography>}

          {couponsQuery.isError && (
            <Alert severity="error">
              {couponsQuery.error instanceof Error
                ? couponsQuery.error.message
                : "Failed to load coupons."}
            </Alert>
          )}

          {!couponsQuery.isLoading &&
            !couponsQuery.isError &&
            (couponsQuery.data?.length ?? 0) === 0 && (
              <Typography color="text.secondary">No coupons yet.</Typography>
            )}

          <Stack spacing={1}>
            {(couponsQuery.data ?? []).map((coupon) => (
              <Paper
                key={coupon.id}
                component={ListItemButton}
                onClick={() => setSelectedCouponId(coupon.id)}
                sx={(theme) => ({
                  p: 1.5,
                  backgroundColor: theme.appColors.accent.soft,
                  borderColor: theme.appColors.border.muted,
                })}
              >
                <Stack
                  direction={{ xs: "column", sm: "row" }}
                  spacing={1}
                  justifyContent="space-between"
                >
                  <Box>
                    <Typography sx={{ fontWeight: 800 }}>
                      {roundTypeLabel(coupon.roundType)} - Round #{coupon.roundId}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Created {formatRoundDateTime(coupon.createdAt)}
                    </Typography>
                  </Box>
                  <Stack spacing={0.25} alignItems={{ xs: "flex-start", sm: "flex-end" }}>
                    <Typography sx={{ fontWeight: 800 }}>
                      {coupon.status}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Price: {coupon.totalCost}
                    </Typography>
                  </Stack>
                </Stack>
              </Paper>
            ))}
          </Stack>

          {selectedCouponQuery.isLoading && (
            <Box sx={{ mt: 2 }}>
              <CircularProgress size={22} />
            </Box>
          )}

          {selectedCouponQuery.isError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {selectedCouponQuery.error instanceof Error
                ? selectedCouponQuery.error.message
                : "Failed to load coupon."}
            </Alert>
          )}

          {selectedCouponQuery.data && (
            <CouponDetail coupon={selectedCouponQuery.data} />
          )}
        </Paper>

        {creatorOpen && (
          <Paper sx={{ overflow: "hidden" }}>
            <Box sx={{ p: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 900 }}>
                Create coupon
              </Typography>
              <Typography color="text.secondary">
                Pick an upcoming round, then choose one or more outcomes for every
                match.
              </Typography>
            </Box>

            <Divider />

            <Box
              sx={{
                display: "grid",
                gridTemplateColumns: { xs: "1fr", md: "320px 1fr" },
                minHeight: 360,
              }}
            >
              <Box sx={(theme) => ({ borderRight: { md: `1px solid ${theme.appColors.border.subtle}` } })}>
                {roundsQuery.isLoading && (
                  <Box sx={{ p: 2 }}>
                    <CircularProgress size={22} />
                  </Box>
                )}

                {roundsQuery.data?.kind === "server-error" && (
                  <Alert severity="error">
                    Server error {roundsQuery.data.status}: {roundsQuery.data.message}
                  </Alert>
                )}

                {roundsQuery.data?.kind === "network-error" && (
                  <Alert severity="error">
                    Network error: {roundsQuery.data.message}
                  </Alert>
                )}

                {!roundsQuery.isLoading && upcomingRounds.length === 0 && (
                  <Box sx={{ p: 2 }}>
                    <Typography color="text.secondary">
                      No upcoming rounds available.
                    </Typography>
                  </Box>
                )}

                <List disablePadding>
                  {upcomingRounds.map((round) => (
                    <ListItemButton
                      key={round.id}
                      selected={selectedRound?.id === round.id}
                      onClick={() => selectRound(round)}
                    >
                      <ListItemText
                        primary={roundListLabel(round)}
                        secondary={
                          couponRoundIds.has(round.id)
                            ? "Coupon already created"
                            : `Round #${round.id}`
                        }
                      />
                    </ListItemButton>
                  ))}
                </List>
              </Box>

              <Box>
                {!selectedRound && (
                  <Box sx={{ p: 2 }}>
                    <Typography color="text.secondary">
                      Select a round to start building your coupon.
                    </Typography>
                  </Box>
                )}

                {selectedRound && (
                  <Box>
                    <Box sx={{ p: 2 }}>
                      <Typography sx={{ fontWeight: 900 }}>
                        {roundListLabel(selectedRound)}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Select at least one outcome for each match.
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Your estimates start at 33% - 34% - 33% and are used as
                        one temporary provider input.
                      </Typography>
                    </Box>

                    <Divider />

                    {selectedRound.matches.map((match) => (
                      <CouponMatchRow
                        key={match.matchNumber}
                        match={match}
                        internal={
                          internalProbabilitiesByMatch[String(match.matchNumber)] ??
                          null
                        }
                        userProbability={
                          userProbabilities[match.matchNumber] ??
                          DEFAULT_USER_PROBABILITY
                        }
                        selected={selections[match.matchNumber] ?? []}
                        onToggle={(outcome) =>
                          toggleOutcome(match.matchNumber, outcome)
                        }
                        onProbabilityChange={(probability) =>
                          updateUserProbability(match.matchNumber, probability)
                        }
                        onProbabilityReset={() =>
                          resetUserProbability(match.matchNumber)
                        }
                      />
                    ))}

                    <Box sx={{ p: 2 }}>
                      <Typography sx={{ mb: 2, fontWeight: 800 }}>
                        Price: {draftPrice == null ? "N/A" : draftPrice}
                      </Typography>

                      {couponRoundIds.has(selectedRound.id) && (
                        <Alert severity="info" sx={{ mb: 2 }}>
                          You already have a coupon for this round.
                        </Alert>
                      )}

                      {createMutation.isError && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                          {createMutation.error instanceof Error
                            ? createMutation.error.message
                            : "Failed to create coupon."}
                        </Alert>
                      )}

                      {providerPredictionsQuery.data?.kind === "server-error" && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                          Provider prediction error {providerPredictionsQuery.data.status}:{" "}
                          {providerPredictionsQuery.data.message}
                        </Alert>
                      )}

                      {providerPredictionsQuery.data?.kind === "network-error" && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                          Provider prediction network error:{" "}
                          {providerPredictionsQuery.data.message}
                        </Alert>
                      )}

                      {modelSelectionMutation.isError && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                          {modelSelectionMutation.error instanceof Error
                            ? modelSelectionMutation.error.message
                            : "Failed to get value picks."}
                        </Alert>
                      )}

                      <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
                        <Button
                          variant="outlined"
                          onClick={getValueSelections}
                          disabled={!canGetValuePicks}
                        >
                          {modelSelectionMutation.isPending
                            ? "Getting picks..."
                            : "Get value picks"}
                        </Button>

                        <Button
                          variant="contained"
                          onClick={submitCoupon}
                          disabled={!canSubmit}
                        >
                          {createMutation.isPending ? "Submitting..." : "Submit"}
                        </Button>
                      </Stack>
                    </Box>
                  </Box>
                )}
              </Box>
            </Box>
          </Paper>
        )}
      </Stack>
    </Page>
  );
}

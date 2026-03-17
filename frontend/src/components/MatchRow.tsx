// src/components/MatchRow.tsx
import { Box, Stack, Typography } from "@mui/material";
import { SelectionBox } from "./SelectionBox";
import type { Outcome } from "../types/modelRun";
import type { TripleView } from "../types/round";
import type { LiveMatchUpdate } from "../types/live";
import { formatTimeOnly } from "../utils/time";

type Props = {
  index: number;
  home: string;
  away: string;
  kickoff: string;

  // model selection set for this match
  selected: Outcome[];

  // contexts
  market: TripleView | null;
  publicPick: TripleView | null;

  // NEW: live score/status (optional)
  live?: LiveMatchUpdate | null;

  onSelectionClick?: () => void; // public page: opens login dialog
};

function pct(n: number | null): number | null {
  if (n == null) return null;
  return Math.round(n * 100);
}

function isSelected(sel: Outcome[], o: Outcome) {
  return sel.includes(o);
}

function liveStatusLabel(live: LiveMatchUpdate): string | null {
  if (!live.status) return null;

  // prefer minute for live halves
  if (
    live.minute != null &&
    (live.status === "1H" || live.status === "2H" || live.status === "ET")
  ) {
    return `${live.minute}'`;
  }

  // HT/FT/etc
  return live.status;
}

export function MatchRow({
  index,
  home,
  away,
  kickoff,
  selected,
  market,
  publicPick,
  live,
  onSelectionClick,
}: Props) {
  const kickoffText = formatTimeOnly(kickoff) ?? kickoff;

  const hasLiveScore =
    !!live &&
    live.homeGoals != null &&
    live.awayGoals != null &&
    (live.status != null || live.minute != null);

  const status = live ? liveStatusLabel(live) : null;

  return (
    <Box
      sx={{
        px: 2,
        py: 1.1,
        display: "grid",
        gridTemplateColumns: "28px 1fr auto",
        gap: 1.5,
        alignItems: "center",
      }}
    >
      <Typography sx={{ fontWeight: 800, opacity: 0.9 }}>{index}</Typography>

      <Box>
        <Typography sx={{ fontWeight: 900 }}>
          {home} <span style={{ opacity: 0.8 }}>–</span> {away}
        </Typography>
        <Typography variant="caption" sx={{ opacity: 0.7 }}>
          {kickoffText}
        </Typography>
      </Box>

      <Stack direction="row" spacing={1} sx={{ justifyContent: "flex-end", alignItems: "center" }}>
        {hasLiveScore && live && (
          <Box
            sx={{
              px: 1,
              py: 0.5,
              borderRadius: 1,
              backgroundColor: "rgba(0,0,0,0.35)",
              minWidth: 56,
              textAlign: "center",
              lineHeight: 1.05,
            }}
          >
            <Typography variant="caption" sx={{ fontWeight: 900 }}>
              {live.homeGoals}–{live.awayGoals}
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.8, display: "block" }}>
              {status ?? ""}
            </Typography>
          </Box>
        )}

        <SelectionBox
          marketPct={pct(market?.homeWin ?? null)}
          publicPct={pct(publicPick?.homeWin ?? null)}
          recommended={isSelected(selected, "HOME_WIN")}
          selected={isSelected(selected, "HOME_WIN")}
          onClick={onSelectionClick}
        />
        <SelectionBox
          marketPct={pct(market?.draw ?? null)}
          publicPct={pct(publicPick?.draw ?? null)}
          recommended={isSelected(selected, "DRAW")}
          selected={isSelected(selected, "DRAW")}
          onClick={onSelectionClick}
        />
        <SelectionBox
          marketPct={pct(market?.awayWin ?? null)}
          publicPct={pct(publicPick?.awayWin ?? null)}
          recommended={isSelected(selected, "AWAY_WIN")}
          selected={isSelected(selected, "AWAY_WIN")}
          onClick={onSelectionClick}
        />
      </Stack>
    </Box>
  );
}
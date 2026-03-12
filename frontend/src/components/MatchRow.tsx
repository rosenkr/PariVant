import { Box, Stack, Typography } from "@mui/material";
import { SelectionBox } from "./SelectionBox";
import type { Outcome } from "../types/modelRun";
import type { TripleView } from "../types/round";

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

  onSelectionClick?: () => void; // public page: opens login dialog
};

function pct(n: number | null): number | null {
  if (n == null) return null;
  return Math.round(n * 100);
}

function isSelected(sel: Outcome[], o: Outcome) {
  return sel.includes(o);
}

export function MatchRow({
  index,
  home,
  away,
  kickoff,
  selected,
  market,
  publicPick,
  onSelectionClick,
}: Props) {
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
          {kickoff}
        </Typography>
      </Box>

      <Stack direction="row" spacing={1} sx={{ justifyContent: "flex-end" }}>
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
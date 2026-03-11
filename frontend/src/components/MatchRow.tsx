import { Box, Stack, Typography } from "@mui/material";
import { SelectionBox } from "./SelectionBox";
import type { Outcome } from "../types/modelRun";

type Props = {
  index: number;
  home: string;
  away: string;
  kickoff: string;
  // selected outcomes from model run for this match
  selected: Outcome[];
  // future: show svf/odds; for now we display only “selected outcomes” as % placeholders
  // You’ll replace these with match_context data later.
  publicPct?: [number, number, number]; // [1, X, 2] as percents
};

function isSelected(sel: Outcome[], o: Outcome) {
  return sel.includes(o);
}

export function MatchRow({ index, home, away, kickoff, selected, publicPct }: Props) {
  return (
    <Box
      sx={{
        px: 2,
        py: 1.2,
        display: "grid",
        gridTemplateColumns: "28px 1fr auto",
        gap: 1.5,
        alignItems: "center",
      }}
    >
      <Typography sx={{ fontWeight: 800, opacity: 0.9 }}>{index}</Typography>

      <Box>
        <Typography sx={{ fontWeight: 800 }}>
          {home} <span style={{ opacity: 0.8 }}>–</span> {away}
        </Typography>
        <Typography variant="caption" sx={{ opacity: 0.7 }}>
          {kickoff}
        </Typography>
      </Box>

      <Box sx={{ display: "grid", gap: 0.6, justifyItems: "end" }}>
        <Stack direction="row" spacing={1}>
          <SelectionBox label="1" selected={isSelected(selected, "HOME_WIN")} />
          <SelectionBox label="X" selected={isSelected(selected, "DRAW")} />
          <SelectionBox label="2" selected={isSelected(selected, "AWAY_WIN")} />
        </Stack>

        {/* Percent row (optional for now) */}
        {publicPct && (
          <Stack direction="row" spacing={1} sx={{ justifyContent: "flex-end" }}>
            <Typography variant="caption" sx={{ width: 44, textAlign: "center", opacity: 0.75 }}>
              {publicPct[0]}%
            </Typography>
            <Typography variant="caption" sx={{ width: 44, textAlign: "center", opacity: 0.75 }}>
              {publicPct[1]}%
            </Typography>
            <Typography variant="caption" sx={{ width: 44, textAlign: "center", opacity: 0.75 }}>
              {publicPct[2]}%
            </Typography>
          </Stack>
        )}
      </Box>
    </Box>
  );
}
import {
  Box,
  Chip,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Typography,
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import type { RoundStatus, RoundType } from "../types/round";
import { formatCountdownTo, formatRoundDateTime } from "../utils/time";

const PRESET_BUDGETS = [32, 64, 128, 256] as const;

type Props = {
  roundType: RoundType;
  setRoundType: (t: RoundType) => void;

  budget: (typeof PRESET_BUDGETS)[number];
  setBudget: (b: (typeof PRESET_BUDGETS)[number]) => void;

  roundId?: number;
  roundStatus?: RoundStatus;
  start?: string;
};

function roundStatusLabel(roundStatus?: RoundStatus): string | undefined {
  if (!roundStatus) return undefined;
  if (roundStatus === "RUNNING") return "LIVE";
  return roundStatus;
}

export function RoundHeader({
  roundType,
  setRoundType,
  budget,
  setBudget,
  roundId,
  roundStatus,
  start,
}: Props) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    if (roundStatus !== "UPCOMING" || !start) return;

    const id = window.setInterval(() => {
      setNow(new Date());
    }, 30_000);

    return () => window.clearInterval(id);
  }, [roundStatus, start]);

  const countdown = useMemo(() => {
    if (roundStatus !== "UPCOMING") return undefined;
    return formatCountdownTo(start, now);
  }, [roundStatus, start, now]);

  const startText = useMemo(() => formatRoundDateTime(start), [start]);
  const shownStatus = roundStatusLabel(roundStatus);

  return (
    <Box
      sx={{
        display: "flex",
        flexWrap: "wrap",
        gap: 2,
        alignItems: "center",
        justifyContent: "space-between",
      }}
    >
      <Stack spacing={0.3}>
        <Typography variant="h4" sx={{ fontWeight: 900, letterSpacing: 0.2 }}>
          Betting Model
        </Typography>

        <Stack direction="row" spacing={1} alignItems="center" sx={{ flexWrap: "wrap" }}>
          {typeof roundId === "number" && <Chip label={`Round #${roundId}`} size="small" />}

          {shownStatus && (
            <Chip
              label={shownStatus}
              size="small"
              color={roundStatus === "RUNNING" ? "warning" : "default"}
            />
          )}

          {countdown && (
            <Chip
              label={countdown}
              size="small"
              color="secondary"
              variant="outlined"
              sx={{ borderColor: "secondary.main" }}
            />
          )}

          {startText && (
            <Typography variant="caption" sx={{ opacity: 0.75 }}>
              Start: {startText}
            </Typography>
          )}
        </Stack>
      </Stack>

      <Stack direction="row" spacing={2} alignItems="center">
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Round</InputLabel>
          <Select
            label="Round"
            value={roundType}
            onChange={(e) => setRoundType(e.target.value as RoundType)}
          >
            <MenuItem value="STRYKTIPSET">STRYKTIPSET</MenuItem>
            <MenuItem value="EUROPATIPSET">EUROPATIPSET</MenuItem>
            <MenuItem value="TOPPTIPSET">TOPPTIPSET</MenuItem>
          </Select>
        </FormControl>

        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Budget</InputLabel>
          <Select
            label="Budget"
            value={budget}
            onChange={(e) =>
              setBudget(Number(e.target.value) as (typeof PRESET_BUDGETS)[number])
            }
          >
            {PRESET_BUDGETS.map((b) => (
              <MenuItem key={b} value={b}>
                {b} SEK
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Stack>
    </Box>
  );
}
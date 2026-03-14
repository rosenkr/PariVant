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
import type { GameType, RoundStatus } from "../types/round";
import { formatCountdownTo, formatRoundDateTime } from "../utils/time";

const PRESET_BUDGETS = [32, 64, 128, 256] as const;

type Props = {
  // undefined means: use backend fallback (/public/current with no gameType param)
  gameType: GameType | undefined;
  setGameType: (t: GameType) => void;

  // when gameType is undefined, backend picks a type; we show it in the dropdown
  selectedGameType?: GameType;

  budget: (typeof PRESET_BUDGETS)[number];
  setBudget: (b: (typeof PRESET_BUDGETS)[number]) => void;

  roundId?: number;
  roundStatus?: RoundStatus;
  start?: string; // backend LocalDateTime string
  end?: string;   // backend LocalDateTime string
};

export function RoundHeader({
  gameType,
  setGameType,
  selectedGameType,
  budget,
  setBudget,
  roundId,
  roundStatus,
  start,
  end,
}: Props) {
  const shownGameType: GameType = gameType ?? selectedGameType ?? "STRYKTIPSET";

  // update clock every 30s only when we need countdown
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
  const endText = useMemo(() => formatRoundDateTime(end), [end]);

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

          {roundStatus && (
            <Chip
              label={roundStatus}
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

          {endText && (
            <Typography variant="caption" sx={{ opacity: 0.75 }}>
              End: {endText}
            </Typography>
          )}
        </Stack>
      </Stack>

      <Stack direction="row" spacing={2} alignItems="center">
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Game</InputLabel>
          <Select
            label="Game"
            value={shownGameType}
            onChange={(e) => setGameType(e.target.value as GameType)}
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
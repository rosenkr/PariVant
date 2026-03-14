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
import type { GameType, RoundStatus } from "../types/round";

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
  start?: string;
  end?: string;
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

          {start && (
            <Typography variant="caption" sx={{ opacity: 0.75 }}>
              Start: {start}
            </Typography>
          )}

          {end && (
            <Typography variant="caption" sx={{ opacity: 0.75 }}>
              End: {end}
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
import { Box, Stack, Typography } from "@mui/material";
import { SelectionBox } from "./SelectionBox";
import type { Outcome } from "../types/modelRun";
import type { LiveMatchUpdate } from "../types/live";
import { formatTimeOnly, parseLocalDateTime } from "../utils/time";

type Props = {
  index: number;
  home: string;
  away: string;
  kickoff: string;
  selected: Outcome[];
  basePick?: Outcome | null;
  live?: LiveMatchUpdate | null;
  isActive: boolean;
  onClick: () => void;
  onSelectionClick?: () => void;
  marketFallbackUsed?: boolean;
  marketFallbackReason?: string | null;
  showSelectionDisabledHintOnHover?: boolean;
  selectionDisabledHintText?: string;
};

type ScoreBorderTone = "none" | "static" | "live";

function toneForOutcome(
  selected: Outcome[],
  basePick: Outcome | null | undefined,
  outcome: Outcome,
): "none" | "base" | "coverage" {
  if (!selected.includes(outcome)) return "none";
  return basePick === outcome ? "base" : "coverage";
}

function liveStatusLabel(live: LiveMatchUpdate): string | null {
  if (!live.status) return null;

  if (
    live.minute != null &&
    (live.status === "1H" || live.status === "2H" || live.status === "ET")
  ) {
    return `${live.minute}'`;
  }

  return live.status;
}

function currentScoreOutcome(
  live: LiveMatchUpdate | null | undefined,
): Outcome | null {
  if (!live || live.homeGoals == null || live.awayGoals == null) return null;

  if (live.homeGoals > live.awayGoals) return "HOME_WIN";
  if (live.homeGoals < live.awayGoals) return "AWAY_WIN";
  return "DRAW";
}

function scoreBorderForOutcome(
  kickoff: string,
  live: LiveMatchUpdate | null | undefined,
  outcome: Outcome,
): ScoreBorderTone {
  const kickoffDate = parseLocalDateTime(kickoff);
  const started = kickoffDate != null && kickoffDate.getTime() <= Date.now();

  const liveOutcome = currentScoreOutcome(live);
  if (started) {
    return liveOutcome === outcome ? "live" : "none";
  }

  return outcome === "DRAW" ? "static" : "none";
}

export function MatchRow({
  index,
  home,
  away,
  kickoff,
  selected,
  basePick = null,
  live,
  isActive,
  onClick,
  onSelectionClick,
  showSelectionDisabledHintOnHover = false,
  selectionDisabledHintText = "Selections can’t be changed from the home page.",
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
      onClick={onClick}
      sx={{
        px: 2,
        py: 1.1,
        display: "grid",
        gridTemplateColumns: "28px 1fr auto",
        gap: 1.5,
        alignItems: "center",
        cursor: "pointer",
        borderLeft: "3px solid",
        borderLeftColor: isActive ? "secondary.main" : "transparent",
        transition: "background-color 140ms ease, border-color 140ms ease",
        "&:hover": {
          backgroundColor: "rgba(255,255,255,0.045)",
        },
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

      <Stack
        direction="row"
        spacing={1}
        sx={{ justifyContent: "flex-end", alignItems: "center" }}
      >
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
            <Typography
              variant="caption"
              sx={{ opacity: 0.8, display: "block" }}
            >
              {status ?? ""}
            </Typography>
          </Box>
        )}

        <SelectionBox
          label="1"
          tone={toneForOutcome(selected, basePick, "HOME_WIN")}
          scoreBorder={scoreBorderForOutcome(kickoff, live, "HOME_WIN")}
          onClick={onSelectionClick}
          showDisabledTooltipOnHover={showSelectionDisabledHintOnHover}
          disabledTooltipTitle={selectionDisabledHintText}
        />
        <SelectionBox
          label="1"
          tone={toneForOutcome(selected, basePick, "DRAW")}
          scoreBorder={scoreBorderForOutcome(kickoff, live, "DRAW")}
          onClick={onSelectionClick}
          showDisabledTooltipOnHover={showSelectionDisabledHintOnHover}
          disabledTooltipTitle={selectionDisabledHintText}
        />
        <SelectionBox
          label="1"
          tone={toneForOutcome(selected, basePick, "AWAY_WIN")}
          scoreBorder={scoreBorderForOutcome(kickoff, live, "AWAY_WIN")}
          onClick={onSelectionClick}
          showDisabledTooltipOnHover={showSelectionDisabledHintOnHover}
          disabledTooltipTitle={selectionDisabledHintText}
        />
      </Stack>
    </Box>
  );
}

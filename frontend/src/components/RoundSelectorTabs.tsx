import { Box, Button, Paper } from "@mui/material";
import type { RoundView } from "../types/round";

type Props = {
  rounds: RoundView[];
  selectedRoundId: number | null;
  onSelectRound: (roundId: number) => void;
};

export function RoundSelectorTabs({
  rounds,
  selectedRoundId,
  onSelectRound,
}: Props) {
  return (
    <Box sx={{ display: "flex", justifyContent: "flex-start" }}>
      <Paper
        sx={{
          px: 1,
          py: 1,
          display: "inline-flex",
          width: "fit-content",
          maxWidth: "100%",
          backgroundColor: "rgba(255,255,255,0.04)",
          borderColor: "rgba(255,255,255,0.10)",
        }}
      >
        <Box
          sx={{
            display: "flex",
            gap: 1,
            flexWrap: "wrap",
            alignItems: "center",
          }}
        >
          {rounds.map((round, index) => {
            const active = round.id === selectedRoundId;

            return (
              <Button
                key={round.id}
                onClick={() => onSelectRound(round.id)}
                variant={active ? "contained" : "outlined"}
                size="small"
                sx={{
                  minWidth: 0,
                  px: 1.5,
                  py: 0.75,
                  borderRadius: 999,
                  textTransform: "none",
                  fontWeight: 800,
                  borderColor: active ? "secondary.main" : "rgba(255,255,255,0.18)",
                  backgroundColor: active ? "secondary.main" : "transparent",
                  color: active ? "#fff" : "text.primary",
                  "&:hover": {
                    borderColor: "secondary.main",
                    backgroundColor: active
                      ? "secondary.main"
                      : "rgba(255,255,255,0.05)",
                  },
                }}
              >
                {`Round ${index + 1}`}
              </Button>
            );
          })}
        </Box>
      </Paper>
    </Box>
  );
}
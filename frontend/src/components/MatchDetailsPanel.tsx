import { Box, Divider, Paper, Stack, Typography } from "@mui/material";
import type { UseQueryResult } from "@tanstack/react-query";
import type { GetRoundProviderPredictionsResult } from "../types/providerPrediction";
import type { ProviderPredictionView } from "../types/providerPrediction";
import type { MatchView, TripleView } from "../types/round";

type ProbabilityTripleDtoShape = {
  homeWin: number;
  draw: number;
  awayWin: number;
};

type Props = {
  match: MatchView | null;
  publicPick: TripleView | null;
  market: TripleView | null;
  providers: ProviderPredictionView[];
  internal: ProbabilityTripleDtoShape | null;
  providerQueryState: UseQueryResult<GetRoundProviderPredictionsResult>;
};

function pct(n: number | null | undefined): string {
  if (n == null) return "—";
  return `${Math.round(n * 100)}%`;
}

function providerFallback(status: string): string {
  return status === "NOT_AVAILABLE" ? "N/A" : status;
}

function SectionRow({
  label,
  home,
  draw,
  away,
}: {
  label: string;
  home: string;
  draw: string;
  away: string;
}) {
  return (
    <Box
      sx={{
        display: "grid",
        gridTemplateColumns: "minmax(110px, 1fr) 52px 52px 52px",
        gap: 1,
        alignItems: "center",
        py: 0.9,
      }}
    >
      <Typography sx={{ fontWeight: 700, opacity: 0.92 }}>{label}</Typography>
      <Typography align="center">{home}</Typography>
      <Typography align="center">{draw}</Typography>
      <Typography align="center">{away}</Typography>
    </Box>
  );
}

export function MatchDetailsPanel({
  match,
  publicPick,
  market,
  providers,
  internal,
  providerQueryState,
}: Props) {
  const providerResult = providerQueryState.data;

  return (
    <Paper
      sx={{
        p: 2,
        backgroundColor: "rgba(255,255,255,0.04)",
        borderColor: "rgba(255,255,255,0.10)",
        minHeight: 360,
        position: { lg: "sticky" },
        top: { lg: 24 },
      }}
    >
      {!match && (
        <Typography color="text.secondary">
          Select a match to view details.
        </Typography>
      )}

      {match && (
        <Stack spacing={2}>
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 900 }}>
              {match.homeTeamName} <span style={{ opacity: 0.8 }}>–</span>{" "}
              {match.awayTeamName}
            </Typography>
            <Typography variant="body2" sx={{ opacity: 0.68 }}>
              Match info
            </Typography>
          </Box>

          <Box>
            <Box
              sx={{
                display: "grid",
                gridTemplateColumns: "minmax(110px, 1fr) 52px 52px 52px",
                gap: 1,
                alignItems: "center",
                pb: 1,
              }}
            >
              <Box />
              <Typography align="center" sx={{ fontWeight: 800 }}>
                1
              </Typography>
              <Typography align="center" sx={{ fontWeight: 800 }}>
                X
              </Typography>
              <Typography align="center" sx={{ fontWeight: 800 }}>
                2
              </Typography>
            </Box>

            <Divider sx={{ borderColor: "rgba(255,255,255,0.08)" }} />

            <SectionRow
              label="Public"
              home={pct(publicPick?.homeWin)}
              draw={pct(publicPick?.draw)}
              away={pct(publicPick?.awayWin)}
            />
            <Divider sx={{ borderColor: "rgba(255,255,255,0.08)" }} />

            <SectionRow
              label="Market"
              home={pct(market?.homeWin)}
              draw={pct(market?.draw)}
              away={pct(market?.awayWin)}
            />
            <Divider sx={{ borderColor: "rgba(255,255,255,0.08)" }} />

            {providers.length > 0 ? (
              <>
                {providers.map((provider) => (
                  <Box key={provider.providerName}>
                    <SectionRow
                      label={provider.providerName}
                      home={
                        provider.status === "OK"
                          ? pct(provider.probabilityHome)
                          : providerFallback(provider.status)
                      }
                      draw={
                        provider.status === "OK"
                          ? pct(provider.probabilityDraw)
                          : providerFallback(provider.status)
                      }
                      away={
                        provider.status === "OK"
                          ? pct(provider.probabilityAway)
                          : providerFallback(provider.status)
                      }
                    />
                    <Divider sx={{ borderColor: "rgba(255,255,255,0.08)" }} />
                  </Box>
                ))}
              </>
            ) : (
              <>
                <Box sx={{ py: 1 }}>
                  <Typography color="text.secondary">
                    {providerQueryState.isLoading
                      ? "Loading providers…"
                      : providerResult?.kind === "server-error"
                        ? `Provider data error ${providerResult.status}: ${providerResult.message}`
                        : providerResult?.kind === "network-error"
                          ? `Provider data network error: ${providerResult.message}`
                          : "No provider predictions available for this match."}
                  </Typography>
                </Box>
                <Divider sx={{ borderColor: "rgba(255,255,255,0.08)" }} />
              </>
            )}

            <SectionRow
              label="Internal"
              home={pct(internal?.homeWin)}
              draw={pct(internal?.draw)}
              away={pct(internal?.awayWin)}
            />
          </Box>
        </Stack>
      )}
    </Paper>
  );
}

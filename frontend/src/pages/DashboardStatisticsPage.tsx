import { Alert, Box, CircularProgress, Paper, Stack, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { getDashboardStats } from "../api/private/dashboardStats";
import { useAuth } from "../auth/AuthContext";
import { Page } from "../components/layout/Page";
import type { DashboardStatBlock } from "../types/dashboardStats";

function StatsCard({
  title,
  stats,
}: {
  title: string;
  stats: DashboardStatBlock;
}) {
  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" sx={{ fontWeight: 900, mb: 2 }}>
        {title}
      </Typography>
      <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
        <Box>
          <Typography color="text.secondary">Win</Typography>
          <Typography color="success.main" sx={{ fontSize: 32, fontWeight: 900 }}>
            {stats.wins}
          </Typography>
        </Box>
        <Box>
          <Typography color="text.secondary">Lose</Typography>
          <Typography color="error.main" sx={{ fontSize: 32, fontWeight: 900 }}>
            {stats.losses}
          </Typography>
        </Box>
      </Stack>
    </Paper>
  );
}

export function DashboardStatisticsPage() {
  const { isAuthenticated, token } = useAuth();

  const statsQuery = useQuery({
    queryKey: ["dashboardStats"],
    queryFn: () => getDashboardStats(token as string),
    enabled: isAuthenticated && token != null,
  });

  if (!isAuthenticated) {
    return null;
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
              Statistics
            </Typography>
            <Typography color="text.secondary">
              Your coupon and confident pick performance.
            </Typography>
          </Box>
        </Stack>

        {statsQuery.isLoading && <CircularProgress size={24} />}

        {statsQuery.isError && (
          <Alert severity="error">
            {statsQuery.error instanceof Error
              ? statsQuery.error.message
              : "Failed to load statistics."}
          </Alert>
        )}

        {statsQuery.data && (
          <Stack spacing={2}>
            <StatsCard title="Your confident picks stats" stats={statsQuery.data.confidentPickStats} />
            <StatsCard title="Your coupons stats" stats={statsQuery.data.couponStats} />
          </Stack>
        )}
      </Stack>
    </Page>
  );
}

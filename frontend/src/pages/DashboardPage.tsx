import DashboardRoundedIcon from "@mui/icons-material/DashboardRounded";
import { Box, Paper, Stack, Typography } from "@mui/material";
import { useAuth } from "../auth/AuthContext";
import { Page } from "../components/layout/Page";

export function DashboardPage() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return null;
  }

  return (
    <Page maxWidth="lg">
      <Stack spacing={3} sx={{ px: { xs: 2, md: 0 }, py: { xs: 2, md: 4 } }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 900 }}>
            Dashboard
          </Typography>
          <Typography color="text.secondary">
            Your Parivant workspace will live here.
          </Typography>
        </Box>

        <Paper
          sx={(theme) => ({
            p: 3,
            backgroundColor: theme.appColors.accent.soft,
            borderColor: theme.appColors.border.muted,
          })}
        >
          <Stack direction="row" spacing={2} alignItems="center">
            <DashboardRoundedIcon color="primary" />
            <Box>
              <Typography sx={{ fontWeight: 800 }}>My coupons</Typography>
              <Typography color="text.secondary">
                Placeholder for your saved coupons and betting history.
              </Typography>
            </Box>
          </Stack>
        </Paper>
      </Stack>
    </Page>
  );
}

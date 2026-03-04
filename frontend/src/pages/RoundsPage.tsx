import { Paper, Stack, Typography } from "@mui/material";
import { Page } from "../components/layout/Page";

export function RoundsPage() {
  return (
    <Page variant="contained">
      <Stack spacing={2}>
        <Typography variant="h4" sx={{ fontWeight: 800 }}>
          Rounds
        </Typography>

        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography>
            Placeholder page. Later this can show browsing of rounds, history,
            etc.
          </Typography>
        </Paper>
      </Stack>
    </Page>
  );
}
import { Paper, Stack, Typography } from "@mui/material";
import { Page } from "../components/layout/Page";

export function AboutPage() {
  return (
    <Page>
      <Stack spacing={2}>
        <Typography variant="h4" sx={{ fontWeight: 800 }}>
          About
        </Typography>

        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography>
            Placeholder page.
          </Typography>
        </Paper>
      </Stack>
    </Page>
  );
}

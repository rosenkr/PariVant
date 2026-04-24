import { Paper, Stack, Typography } from "@mui/material";
import { Page } from "../components/layout/Page";

export function PolicyPage() {
  return (
    <Page maxWidth="md">
      <Stack spacing={3}>
        <Typography variant="h4" sx={{ fontWeight: 800 }}>
          Privacy Policy
        </Typography>

        <Paper variant="outlined" sx={{ p: { xs: 2.5, md: 3 } }}>
          <Stack spacing={2}>
            <Typography variant="body1">
              Add your PariVant Privacy Policy text here.
            </Typography>

            <Typography variant="body1">
              This page is now routed and linked from the footer, so you can
              paste in the final legal text whenever ready.
            </Typography>
          </Stack>
        </Paper>
      </Stack>
    </Page>
  );
}
import { Box, Container, Divider, Stack, Typography } from "@mui/material";
import { Outlet } from "react-router-dom";
import { TopNav } from "./TopNav";

export function AppShell() {
  return (
    <Stack sx={{ minHeight: "100vh" }}>
      <TopNav />

      <Box sx={{ flex: 1 }}>
        <Outlet />
      </Box>

      <Divider />
      <Box component="footer" sx={{ py: 2 }}>
        <Container maxWidth="lg">
          <Typography variant="body2" color="text.secondary" align="center">
            © {new Date().getFullYear()} Svenska Spel Model (local dev)
          </Typography>
        </Container>
      </Box>
    </Stack>
  );
}
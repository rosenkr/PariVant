import { Box, Container, Typography } from "@mui/material";
import { Outlet } from "react-router-dom";
import { TopNav } from "./TopNav";

export function AppShell() {
  return (
    <Box
      sx={(theme) => ({
        minHeight: "100dvh",
        display: "flex",
        flexDirection: "column",
        backgroundColor: theme.appColors.surface.page,
      })}
    >
      <TopNav />

      <Box
        component="main"
        sx={(theme) => ({
          flex: 1,
          width: "100%",
          backgroundColor: theme.appColors.surface.page,
        })}
      >
        <Outlet />
      </Box>

      <Box
        component="footer"
        sx={(theme) => ({
          flexShrink: 0,
          py: 2,
          backgroundColor: theme.appColors.surface.footer,
          borderTop: `1px solid ${theme.appColors.border.subtle}`,
        })}
      >
        <Container maxWidth="lg">
          <Typography variant="body2" color="text.secondary" align="center">
            © {new Date().getFullYear()} Svenska Spel Model (local dev)
          </Typography>
        </Container>
      </Box>
    </Box>
  );
}

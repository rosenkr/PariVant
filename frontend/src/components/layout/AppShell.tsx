import { Box, Container, Typography, Stack } from "@mui/material";
import { Link as RouterLink, Outlet } from "react-router-dom";
import { useState } from "react";
import { TopNav } from "./TopNav";
import { AuthModal } from "../auth/AuthModal";

export function AppShell() {
  const [authModalOpen, setAuthModalOpen] = useState(false);

  return (
    <Box
      sx={(theme) => ({
        minHeight: "100dvh",
        display: "flex",
        flexDirection: "column",
        backgroundColor: theme.appColors.surface.page,
      })}
    >
      <TopNav onOpenAuthModal={() => setAuthModalOpen(true)} />

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
          py: { xs: 4, md: 5 },
          backgroundColor: theme.appColors.surface.footer,
          borderTop: `1px solid ${theme.appColors.border.subtle}`,
        })}
      >
        <Container maxWidth="lg">
          <Stack spacing={2.5} alignItems="center">
            <Stack
              direction="row"
              spacing={1.5}
              alignItems="center"
              justifyContent="center"
            >
              <Box
                component="img"
                src="/favicon.svg"
                alt="PariVant logo"
                sx={{
                  width: 40,
                  height: 40,
                  display: "block",
                }}
              />
              <Typography
                variant="h4"
                sx={(theme) => ({
                  fontWeight: 900,
                  letterSpacing: 0.2,
                  color: theme.appColors.accent.primary,
                  lineHeight: 1,
                })}
              >
                PariVant
              </Typography>
            </Stack>

            <Typography
              variant="h6"
              align="center"
              sx={(theme) => ({
                maxWidth: 720,
                fontWeight: 500,
                color: theme.appColors.text.primary,
                lineHeight: 1.4,
              })}
            >
              Data-driven decision support for smarter Svenska Spel pool
              betting.
            </Typography>

            <Stack
              direction="row"
              spacing={{ xs: 2, sm: 4 }}
              useFlexGap
              flexWrap="wrap"
              justifyContent="center"
            >
              <Typography
                component={RouterLink}
                to="/privacy"
                variant="body1"
                sx={(theme) => ({
                  color: theme.appColors.text.secondary,
                  textDecoration: "none",
                  transition:
                    "color 160ms ease, text-decoration-color 160ms ease",
                  "&:hover": {
                    color: theme.appColors.text.primary,
                    textDecoration: "underline",
                  },
                })}
              >
                Privacy Policy
              </Typography>

              <Typography
                component={RouterLink}
                to="/tos"
                variant="body1"
                sx={(theme) => ({
                  color: theme.appColors.text.secondary,
                  textDecoration: "none",
                  transition:
                    "color 160ms ease, text-decoration-color 160ms ease",
                  "&:hover": {
                    color: theme.appColors.text.primary,
                    textDecoration: "underline",
                  },
                })}
              >
                Terms of Service
              </Typography>

              <Typography
                variant="body1"
                sx={(theme) => ({
                  color: theme.appColors.text.secondary,
                  cursor: "default",
                })}
              >
                Cookie Preferences
              </Typography>
            </Stack>

            <Typography
              variant="body2"
              align="center"
              sx={(theme) => ({
                color: theme.appColors.text.secondary,
              })}
            >
              © {new Date().getFullYear()} A.R. All rights
              reserved.
            </Typography>
          </Stack>
        </Container>
      </Box>

      <AuthModal open={authModalOpen} onClose={() => setAuthModalOpen(false)} />
    </Box>
  );
}

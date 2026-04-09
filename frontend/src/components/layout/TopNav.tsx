import {
  AppBar,
  Box,
  Container,
  IconButton,
  Stack,
  Tab,
  Tabs,
  Toolbar,
  Tooltip,
  Typography,
} from "@mui/material";
import LightModeRoundedIcon from "@mui/icons-material/LightModeRounded";
import DarkModeRoundedIcon from "@mui/icons-material/DarkModeRounded";
import { useLocation, useNavigate } from "react-router-dom";
import { useThemeMode } from "../../hooks/useThemeMode";

type NavItem = { label: string; path: string };

const NAV: NavItem[] = [{ label: "About", path: "/about" }];

function currentTabIndex(pathname: string) {
  const idx = NAV.findIndex((n) => n.path === pathname);
  return idx === -1 ? false : idx;
}

export function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();
  const { mode, toggleMode } = useThemeMode();

  const value = currentTabIndex(location.pathname);
  const isDark = mode === "dark";

  return (
    <AppBar position="sticky" elevation={0}>
      <Container maxWidth="xl">
        <Toolbar
          disableGutters
          sx={{
            minHeight: 72,
            gap: 2,
          }}
        >
          <Box
            sx={{
              flexBasis: { xs: "auto", md: 240 },
              flexShrink: 0,
              minWidth: 0,
            }}
          >
            <Box
              role="button"
              tabIndex={0}
              onClick={() => navigate("/")}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  navigate("/");
                }
              }}
              sx={(theme) => ({
                display: "inline-flex",
                alignItems: "center",
                gap: 0,
                px: 0.5,
                py: 0.5,
                borderRadius: 2,
                cursor: "pointer",
                userSelect: "none",
                "&:focus-visible": {
                  outline: `2px solid ${theme.appColors.accent.primary}`,
                  outlineOffset: 2,
                },
              })}
            >
              <Box
                component="img"
                src="/favicon.svg"
                alt="PariVant logo"
                sx={{
                  width: 30,
                  height: 30,
                  display: "block",
                  flexShrink: 0,
                }}
              />

              <Stack spacing={0} sx={{ lineHeight: 1 }}>
                <Typography
                  sx={(theme) => ({
                    fontSize: "1rem",
                    fontWeight: 900,
                    letterSpacing: 0.2,
                    color: theme.appColors.text.primary,
                    lineHeight: 1,
                  })}
                >
                  Pari
                </Typography>
                <Typography
                  sx={(theme) => ({
                    fontSize: "1rem",
                    fontWeight: 900,
                    letterSpacing: 0.2,
                    color: theme.appColors.text.primary,
                    lineHeight: 1,
                  })}
                >
                  Vant
                </Typography>
              </Stack>
            </Box>
          </Box>

          <Box
            sx={{
              flex: 1,
              display: "flex",
              justifyContent: "center",
              minWidth: 0,
            }}
          >
            <Tabs
              value={value}
              onChange={(_, next) => navigate(NAV[next].path)}
              textColor="inherit"
              indicatorColor="primary"
              sx={(theme) => ({
                minHeight: 48,
                "& .MuiTabs-flexContainer": {
                  gap: { xs: 0.5, sm: 1.5 },
                },
                "& .MuiTab-root": {
                  minHeight: 48,
                  textTransform: "none",
                  fontWeight: 700,
                  color: theme.appColors.text.secondary,
                  px: { xs: 1.25, sm: 2 },
                  borderRadius: 999,
                },
                "& .Mui-selected": {
                  color: theme.appColors.text.primary,
                },
                "& .MuiTabs-indicator": {
                  height: 3,
                  borderRadius: 999,
                  backgroundColor: theme.appColors.accent.primary,
                },
              })}
            >
              {NAV.map((n) => (
                <Tab key={n.path} label={n.label} />
              ))}
            </Tabs>
          </Box>

          <Box
            sx={{
              flexBasis: { xs: "auto", md: 240 },
              flexShrink: 0,
              display: "flex",
              justifyContent: "flex-end",
              alignItems: "center",
            }}
          >
            <Tooltip
              title={isDark ? "Switch to light mode" : "Switch to dark mode"}
            >
              <IconButton
                onClick={toggleMode}
                aria-label={
                  isDark ? "Switch to light mode" : "Switch to dark mode"
                }
                sx={(theme) => {
                  const toggleColors = isDark
                    ? theme.appColors.modeToggle.sun
                    : theme.appColors.modeToggle.moon;

                  return {
                    width: 40,
                    height: 40,
                    borderRadius: 1.5,
                    border: `1px solid ${toggleColors.border}`,
                    backgroundColor: toggleColors.background,
                    color: toggleColors.color,
                    "&:hover": {
                      backgroundColor: toggleColors.hoverBackground,
                      borderColor: toggleColors.hoverBorder,
                    },
                  };
                }}
              >
                {isDark ? (
                  <LightModeRoundedIcon fontSize="small" />
                ) : (
                  <DarkModeRoundedIcon fontSize="small" />
                )}
              </IconButton>
            </Tooltip>
          </Box>
        </Toolbar>
      </Container>
    </AppBar>
  );
}

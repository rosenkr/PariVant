import {
  AppBar,
  Box,
  Container,
  IconButton,
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

const NAV: NavItem[] = [
  { label: "Home", path: "/" },
  { label: "Rounds", path: "/rounds" },
];

function currentTabIndex(pathname: string) {
  const idx = NAV.findIndex((n) => n.path === pathname);
  return idx === -1 ? 0 : idx;
}

export function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();
  const { mode, toggleMode } = useThemeMode();

  const value = currentTabIndex(location.pathname);
  const isDark = mode === "dark";

  return (
    <AppBar position="sticky" elevation={0}>
      <Container maxWidth="lg">
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
            <Typography
              variant="h6"
              sx={(theme) => ({
                fontWeight: 800,
                letterSpacing: 0.2,
                color: theme.appColors.text.primary,
                whiteSpace: "nowrap",
              })}
            >
              Svenska Spel Model
            </Typography>
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
            <Tooltip title={isDark ? "Switch to light mode" : "Switch to dark mode"}>
              <IconButton
                onClick={toggleMode}
                aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
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
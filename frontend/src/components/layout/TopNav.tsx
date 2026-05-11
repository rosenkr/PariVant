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
  FormControl,
  MenuItem,
  Select,
  type SelectChangeEvent,
} from "@mui/material";
import LightModeRoundedIcon from "@mui/icons-material/LightModeRounded";
import DarkModeRoundedIcon from "@mui/icons-material/DarkModeRounded";
import { useLocation, useNavigate } from "react-router-dom";
import { useThemeMode } from "../../hooks/useThemeMode";
import LanguageRoundedIcon from "@mui/icons-material/LanguageRounded";
import AccountCircleRoundedIcon from "@mui/icons-material/AccountCircleRounded";
import { useState } from "react";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import {useAuth} from "../../auth/AuthContext.tsx";
import { UserGreeting } from "../UserGreeting.tsx";

type NavItem = { label: string; path: string };

const NAV: NavItem[] = [{ label: "About", path: "/about" }];

function currentTabIndex(pathname: string): number | false {
  const idx = NAV.findIndex((n) => n.path === pathname);
  return idx === -1 ? false : idx;
}

type Props = {
  onOpenAuthModal: () => void;
};

export function TopNav({ onOpenAuthModal }: Props) {
  const location = useLocation();
  const navigate = useNavigate();
  const { mode, toggleMode } = useThemeMode();

  const value = currentTabIndex(location.pathname);
  const isDark = mode === "dark";
  const [language, setLanguage] = useState("English");

  const {user, isAuthenticated} = useAuth();

  const handleLanguageChange = (event: SelectChangeEvent) => {
    setLanguage(event.target.value);
  };

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
              onClick={() => {
                void navigate("/");
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  void navigate("/");
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
              onChange={(_, next: number) => {
                const nextItem = NAV[next];
                if (nextItem) {
                  void navigate(nextItem.path);
                }
              }}
              textColor="inherit"
              indicatorColor="primary"
              sx={(theme) => ({
                minHeight: 48,
                "& .MuiTabs-flexContainer": {
                  gap: { xs: 0.5, sm: 1.5 },
                },
                "& .MuiTab-root": {
                  minHeight: 48,
                  minWidth: 0,
                  textTransform: "none",
                  fontWeight: 700,
                  color: theme.appColors.nav.item.color,
                  opacity: 1,
                  px: { xs: 1.25, sm: 2 },
                  borderRadius: 999,
                  transition: "background-color 160ms ease, color 160ms ease",
                  "&:hover": {
                    color: theme.appColors.nav.item.hoverColor,
                    backgroundColor: theme.appColors.nav.item.hoverBackground,
                  },
                },
                "& .Mui-selected": {
                  color: `${theme.appColors.nav.item.activeColor} !important`,
                  backgroundColor: theme.appColors.nav.item.activeBackground,
                },
                "& .MuiTabs-indicator": {
                  height: 3,
                  borderRadius: 999,
                  backgroundColor: theme.appColors.accent.primary,
                },
              })}
            >
              {NAV.map((n) => (
                <Tab
                  key={n.path}
                  icon={
                    n.label === "About" ? (
                      <InfoOutlinedIcon sx={{ fontSize: 18 }} />
                    ) : undefined
                  }
                  iconPosition="start"
                  label={n.label}
                />
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
            <Stack direction="row" spacing={1} alignItems="center">
                {isAuthenticated && user && <UserGreeting user={user} />}
                <Tooltip title="Log in or create account">
                <IconButton
                  aria-label="Log in or create account"
                  onClick={onOpenAuthModal}
                  sx={(theme) => ({
                    width: 40,
                    height: 40,
                    borderRadius: 1.5,
                    color: theme.appColors.text.secondary,
                    "&:hover": {
                      borderColor: theme.appColors.border.strong,
                      backgroundColor: theme.appColors.accent.soft,
                    },
                  })}
                >
                  <AccountCircleRoundedIcon fontSize="medium" />
                </IconButton>
              </Tooltip>

              <FormControl size="small">
                <Select
                  value={language}
                  onChange={handleLanguageChange}
                  variant="outlined"
                  displayEmpty
                  startAdornment={
                    <LanguageRoundedIcon
                      sx={(theme) => ({
                        mr: 1,
                        fontSize: 18,
                        color: theme.appColors.text.secondary,
                      })}
                    />
                  }
                  sx={(theme) => ({
                    minWidth: 138,
                    height: 40,
                    borderRadius: 1.5,
                    color: theme.appColors.text.primary,
                    fontWeight: 600,
                    "& .MuiOutlinedInput-notchedOutline": {
                      border: "none",
                    },
                    "&:hover .MuiOutlinedInput-notchedOutline": {
                      border: "none",
                    },
                    "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
                      border: "none",
                    },
                    ".MuiSelect-select": {
                      display: "flex",
                      alignItems: "center",
                      py: 1,
                    },
                    ".MuiSvgIcon-root": {
                      color: theme.appColors.text.secondary,
                    },
                  })}
                >
                  <MenuItem value="English">English</MenuItem>
                  <MenuItem value="Svenska">Svenska</MenuItem>
                </Select>
              </FormControl>

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
            </Stack>
          </Box>
        </Toolbar>
      </Container>
    </AppBar>
  );
}

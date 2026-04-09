import React, { useEffect, useMemo, useState } from "react";
import { CssBaseline } from "@mui/material";
import type { PaletteMode } from "@mui/material";
import { ThemeProvider } from "@mui/material/styles";
import { getAppTheme } from "./index";
import { ThemeModeContext, type ThemeModeContextValue } from "./themeModeContext";

const STORAGE_KEY = "app-color-mode";

function getInitialMode(): PaletteMode {
  if (typeof window === "undefined") {
    return "dark";
  }

  const stored = window.localStorage.getItem(STORAGE_KEY);
  return stored === "light" || stored === "dark" ? stored : "dark";
}

export function AppThemeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setMode] = useState<PaletteMode>(getInitialMode);

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEY, mode);
  }, [mode]);

  const value = useMemo<ThemeModeContextValue>(
    () => ({
      mode,
      toggleMode: () => {
        setMode((prev) => (prev === "dark" ? "light" : "dark"));
      },
      setMode,
    }),
    [mode],
  );

  const theme = useMemo(() => getAppTheme(mode), [mode]);

  return (
    <ThemeModeContext.Provider value={value}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ThemeModeContext.Provider>
  );
}
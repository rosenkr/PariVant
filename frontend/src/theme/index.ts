import { createTheme } from "@mui/material/styles";
import type { PaletteMode } from "@mui/material";
import { getColors, type AppColors } from "./colors";

declare module "@mui/material/styles" {
  interface Theme {
    appColors: AppColors;
  }

  interface ThemeOptions {
    appColors?: AppColors;
  }
}

export function getAppTheme(mode: PaletteMode) {
  const colors = getColors(mode);

  return createTheme({
    appColors: colors,

    palette: {
      mode,
      primary: {
        main: colors.accent.primary,
        contrastText: colors.accent.contrastText,
      },
      secondary: {
        main: colors.live.primary,
        contrastText: colors.live.contrastText,
      },
      success: {
        main: colors.status.success,
      },
      warning: {
        main: colors.status.warning,
      },
      error: {
        main: colors.status.error,
      },
      info: {
        main: colors.status.info,
      },
      background: {
        default: colors.surface.background,
        paper: colors.surface.paper,
      },
      text: {
        primary: colors.text.primary,
        secondary: colors.text.secondary,
      },
      divider: colors.border.subtle,
    },

    shape: {
      borderRadius: 12,
    },

    typography: {
      fontFamily:
        "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif",
    },

    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: {
            backgroundColor: colors.surface.background,
            color: colors.text.primary,
          },
        },
      },

      MuiPaper: {
        defaultProps: {
          variant: "outlined",
        },
        styleOverrides: {
          root: {
            backgroundImage: "none",
            border: `1px solid ${colors.border.subtle}`,
          },
        },
      },

      MuiCard: {
        styleOverrides: {
          root: {
            backgroundImage: "none",
            border: `1px solid ${colors.border.subtle}`,
          },
        },
      },

      MuiButtonBase: {
        defaultProps: {
          disableRipple: true,
        },
      },

      MuiDivider: {
        styleOverrides: {
          root: {
            borderColor: colors.border.subtle,
          },
        },
      },

      MuiChip: {
        styleOverrides: {
          root: {
            borderColor: colors.border.muted,
          },
        },
      },

      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundImage: "none",
            backgroundColor: colors.surface.paper,
            color: colors.text.primary,
            borderBottom: `1px solid ${colors.border.subtle}`,
          },
        },
      },

      MuiTextField: {
        defaultProps: {
          variant: "outlined",
          fullWidth: true,
        },
      },

      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            backgroundColor: colors.auth.fieldBackground,
            borderRadius: 14,
            transition: "box-shadow 160ms ease, border-color 160ms ease",
            "& .MuiOutlinedInput-notchedOutline": {
              borderColor: colors.auth.fieldBorder,
            },
            "&:hover .MuiOutlinedInput-notchedOutline": {
              borderColor: colors.auth.fieldBorderHover,
            },
            "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
              borderColor: colors.auth.fieldBorderFocus,
              borderWidth: 2,
            },
          },
        },
      },

      MuiInputLabel: {
        styleOverrides: {
          root: {
            "&.Mui-focused": {
              color: colors.auth.fieldBorderFocus,
            },
          },
        },
      },

      MuiDialog: {
        styleOverrides: {
          paper: {
            backgroundImage: "none",
          },
        },
      },

      MuiButton: {
        styleOverrides: {
          containedPrimary: {
            boxShadow: "none",
          },
        },
      },
    },
  });
}
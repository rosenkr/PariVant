import type { PaletteMode } from "@mui/material";

export type AppColors = {
  surface: {
    background: string;
    page: string;
    paper: string;
    panel: string;
    raised: string;
  };
  border: {
    subtle: string;
    muted: string;
    strong: string;
    accent: string;
  };
  text: {
    primary: string;
    secondary: string;
    muted: string;
    inverse: string;
  };
  accent: {
    primary: string;
    hover: string;
    soft: string;
    contrastText: string;
  };
  live: {
    primary: string;
    border: string;
    soft: string;
    glow: string;
    contrastText: string;
  };
  pick: {
    base: string;
    coverage: string;
    softBase: string;
    softCoverage: string;
    contrastText: string;
  };
  status: {
    success: string;
    warning: string;
    error: string;
    info: string;
  };
};

const darkMode: AppColors = {
  surface: {
    background: "#4f5558", // nardo-inspired app background
    page: "#5a6164",
    paper: "#676f73",
    panel: "#727b80",
    raised: "#7d878c",
  },
  border: {
    subtle: "rgba(165, 243, 252, 0.16)",
    muted: "rgba(165, 243, 252, 0.24)",
    strong: "rgba(165, 243, 252, 0.4)",
    accent: "#67e8f9",
  },
  text: {
    primary: "#f5f7f8",
    secondary: "#d9e0e3",
    muted: "#bac4c8",
    inverse: "#111315",
  },
  accent: {
    primary: "#67e8f9",
    hover: "#22d3ee",
    soft: "rgba(103, 232, 249, 0.16)",
    contrastText: "#0b1418",
  },
  live: {
    primary: "#22d3ee",
    border: "#67e8f9",
    soft: "rgba(34, 211, 238, 0.14)",
    glow: "rgba(34, 211, 238, 0.34)",
    contrastText: "#061014",
  },
  pick: {
    base: "#c96a00",
    coverage: "#f59e0b",
    softBase: "rgba(201, 106, 0, 0.2)",
    softCoverage: "rgba(245, 158, 11, 0.2)",
    contrastText: "#111315",
  },
  status: {
    success: "#34d399",
    warning: "#f59e0b",
    error: "#f87171",
    info: "#67e8f9",
  },
};

const lightMode: AppColors = {
  surface: {
    background: "#f3f5f4",
    page: "#eef1ef",
    paper: "#ffffff",
    panel: "#f7f9f8",
    raised: "#ffffff",
  },
  border: {
    subtle: "rgba(22, 101, 52, 0.12)",
    muted: "rgba(22, 101, 52, 0.18)",
    strong: "rgba(22, 101, 52, 0.3)",
    accent: "#166534",
  },
  text: {
    primary: "#101514",
    secondary: "#2f3a37",
    muted: "#55635e",
    inverse: "#f8faf9",
  },
  accent: {
    primary: "#166534",
    hover: "#14532d",
    soft: "rgba(22, 101, 52, 0.1)",
    contrastText: "#f8faf9",
  },
  live: {
    primary: "#0f766e",
    border: "#0f766e",
    soft: "rgba(15, 118, 110, 0.1)",
    glow: "rgba(15, 118, 110, 0.2)",
    contrastText: "#f8faf9",
  },
  pick: {
    base: "#b45309",
    coverage: "#d97706",
    softBase: "rgba(180, 83, 9, 0.14)",
    softCoverage: "rgba(217, 119, 6, 0.14)",
    contrastText: "#101514",
  },
  status: {
    success: "#15803d",
    warning: "#b45309",
    error: "#b91c1c",
    info: "#166534",
  },
};

export function getColors(mode: PaletteMode): AppColors {
  return mode === "light" ? lightMode : darkMode;
}
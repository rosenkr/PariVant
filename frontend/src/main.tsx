import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { CssBaseline } from "@mui/material";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import App from "./App";

const queryClient = new QueryClient();

const theme = createTheme({
  palette: {
    mode: "dark",
    background: {
      default: "#2b2b2b",
      paper: "#1f1f1f",
    },
    primary: { main: "#002B36" },
    secondary: { main: "#ff2d8e" },
    warning: { main: "#ff7a00" },
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, Arial",
  },
  components: {
    MuiPaper: {
      styleOverrides: {
        root: { border: "1px solid rgba(255,255,255,0.08)" },
      },
    },
    MuiButtonBase: { defaultProps: { disableRipple: true } },
  },
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <App />
        </ThemeProvider>
      </QueryClientProvider>
    </BrowserRouter>
  </React.StrictMode>,
);

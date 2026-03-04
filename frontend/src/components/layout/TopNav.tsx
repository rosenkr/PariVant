import { AppBar, Toolbar, Typography, Tabs, Tab, Box } from "@mui/material";
import { useLocation, useNavigate } from "react-router-dom";

type NavItem = { label: string; path: string };

const NAV: NavItem[] = [
  { label: "Home", path: "/" },
  { label: "Rounds", path: "/rounds" }, // placeholder for later
];

function currentTabIndex(pathname: string) {
  const idx = NAV.findIndex((n) => n.path === pathname);
  return idx === -1 ? 0 : idx;
}

export function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();

  const value = currentTabIndex(location.pathname);

  return (
    <AppBar position="sticky" elevation={1}>
      <Toolbar sx={{ gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: 700 }}>
          Svenska Spel Model
        </Typography>

        <Box sx={{ flex: 1 }} />

        <Tabs
          value={value}
          onChange={(_, next) => navigate(NAV[next].path)}
          textColor="inherit"
          indicatorColor="secondary"
          sx={{
            "& .MuiTab-root": { textTransform: "none", fontWeight: 600 },
          }}
        >
          {NAV.map((n) => (
            <Tab key={n.path} label={n.label} />
          ))}
        </Tabs>
      </Toolbar>
    </AppBar>
  );
}
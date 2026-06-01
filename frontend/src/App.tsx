import { Routes, Route } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import HomePage from "./pages/HomePage";
import { AboutPage } from "./pages/AboutPage";
import { TermsPage } from "./pages/TermsPage";
import { PolicyPage } from "./pages/PolicyPage";
import { DashboardPage } from "./pages/DashboardPage";
import { DashboardStatisticsPage } from "./pages/DashboardStatisticsPage";

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/dashboard/statistics" element={<DashboardStatisticsPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/tos" element={<TermsPage />} />
        <Route path="/privacy" element={<PolicyPage />} />
      </Route>
    </Routes>
  );
}

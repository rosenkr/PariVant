import { Routes, Route } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import HomePage from "./pages/HomePage";
import { RoundsPage } from "./pages/RoundsPage";

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/rounds" element={<RoundsPage />} />
      </Route>
    </Routes>
  );
}
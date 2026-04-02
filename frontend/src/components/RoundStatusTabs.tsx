import { Tab, Tabs } from "@mui/material";
import type { RoundStatus } from "../types/round";

type Props = {
  value: RoundStatus;
  onChange: (status: RoundStatus) => void;
};

export function RoundStatusTabs({ value, onChange }: Props) {
  return (
    <Tabs
      value={value}
      onChange={(_, nextValue: RoundStatus) => onChange(nextValue)}
      variant="fullWidth"
      sx={{
        minHeight: 44,
        "& .MuiTabs-indicator": {
          height: 3,
          borderRadius: 999,
        },
      }}
    >
      <Tab value="UPCOMING" label="Upcoming" />
      <Tab value="RUNNING" label="Live" />
      <Tab value="ENDED" label="Ended" />
    </Tabs>
  );
}
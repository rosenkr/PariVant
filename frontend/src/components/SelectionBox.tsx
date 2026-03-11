import { Box, Typography } from "@mui/material";

type Props = {
  label: string;
  selected: boolean;
};

export function SelectionBox({ label, selected }: Props) {
  return (
    <Box
      sx={{
        width: 44,
        height: 32,
        display: "grid",
        placeItems: "center",
        borderRadius: 1,
        border: "1px solid",
        borderColor: selected ? "warning.main" : "rgba(255,255,255,0.25)",
        backgroundColor: selected ? "warning.main" : "transparent",
        color: selected ? "black" : "text.primary",
        fontWeight: 800,
        userSelect: "none",
      }}
    >
      <Typography sx={{ fontWeight: 800, lineHeight: 1 }}>{label}</Typography>
    </Box>
  );
}
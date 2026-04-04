import { Box, Typography, useTheme } from "@mui/material";

type Props = {
  label: "1" | "X" | "2";
  recommended: boolean;
  selected: boolean;
  onClick?: () => void;
};

export function SelectionBox({ label, recommended, selected, onClick }: Props) {
  const theme = useTheme();

  const borderColor = recommended
    ? theme.palette.secondary.main
    : "rgba(255,255,255,0.25)";
  const bg = selected ? theme.palette.warning.main : "transparent";
  const fg = selected ? "black" : theme.palette.text.primary;

  return (
    <Box
      role={onClick ? "button" : undefined}
      onClick={(e) => {
        e.stopPropagation();
        onClick?.();
      }}
      sx={{
        width: 54,
        height: 40,
        display: "grid",
        placeItems: "center",
        borderRadius: 1,
        border: "2px solid",
        borderColor,
        backgroundColor: bg,
        color: fg,
        cursor: onClick ? "pointer" : "default",
        userSelect: "none",
      }}
    >
      <Typography sx={{ fontWeight: 900, lineHeight: 1 }}>{label}</Typography>
    </Box>
  );
}
import { Box, Stack, Typography, useTheme } from "@mui/material";

type Props = {
  marketPct: number | null;
  publicPct: number | null;

  // pink border = model recommendation
  recommended: boolean;

  // orange fill = current selection (on public page: same as recommended)
  selected: boolean;

  onClick?: () => void;
};

export function SelectionBox({
  marketPct,
  publicPct,
  recommended,
  selected,
  onClick,
}: Props) {
  const theme = useTheme();

  const borderColor = recommended ? theme.palette.secondary.main : "rgba(255,255,255,0.25)";
  const bg = selected ? theme.palette.warning.main : "transparent";
  const fg = selected ? "black" : theme.palette.text.primary;

  return (
    <Box
      role={onClick ? "button" : undefined}
      onClick={onClick}
      sx={{
        width: 74,
        height: 46,
        px: 1,
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
      <Stack spacing={0.2} sx={{ width: "100%" }}>
        <Typography variant="caption" sx={{ fontWeight: 800, lineHeight: 1, opacity: 0.9 }}>
          M: {marketPct == null ? "—" : `${marketPct}%`}
        </Typography>
        <Typography variant="caption" sx={{ fontWeight: 800, lineHeight: 1, opacity: 0.9 }}>
          P: {publicPct == null ? "—" : `${publicPct}%`}
        </Typography>
      </Stack>
    </Box>
  );
}
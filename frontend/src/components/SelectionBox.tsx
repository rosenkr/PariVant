import { Box, Typography } from "@mui/material";

type SelectionTone = "none" | "base" | "coverage";
type ScoreBorderTone = "none" | "static" | "live";

type Props = {
  label: string;
  tone?: SelectionTone;
  scoreBorder?: ScoreBorderTone;
  onClick?: () => void;
};

export function SelectionBox({
  label,
  tone = "none",
  scoreBorder = "none",
  onClick,
}: Props) {
  const isSelected = tone !== "none";
  const isBase = tone === "base";

  const borderColor =
    scoreBorder === "none" ? "rgba(255,255,255,0.22)" : "rgba(255,45,142,0.9)";

  const liveBorderSx =
    scoreBorder === "live"
      ? {
          backgroundImage:
            "linear-gradient(120deg, rgba(255,45,142,0.95), rgba(255,110,199,0.95), rgba(255,45,142,0.95))",
          backgroundSize: "220% 220%",
          animation: "scoreBorderShift 2.2s linear infinite",
          "@keyframes scoreBorderShift": {
            "0%": { backgroundPosition: "0% 50%" },
            "100%": { backgroundPosition: "200% 50%" },
          },
          padding: "1.5px",
        }
      : null;

  return (
    <Box
      onClick={(e) => {
        e.stopPropagation();
        onClick?.();
      }}
      sx={{
        width: 34,
        height: 34,
        borderRadius: "50%",
        cursor: onClick ? "pointer" : "default",
        userSelect: "none",
        transition: "all 140ms ease",
        ...(liveBorderSx ?? {}),
      }}
    >
      <Box
        sx={{
          width: "100%",
          height: "100%",
          borderRadius: "50%",
          display: "grid",
          placeItems: "center",
          border: scoreBorder === "live" ? "none" : `1px solid ${borderColor}`,
          backgroundColor: isSelected
            ? isBase
              ? "rgba(255, 140, 0, 0.99)"
              : "rgba(255, 184, 91, 0.88)"
            : "rgba(0,0,0,0)",
          boxShadow: isSelected
            ? isBase
              ? "inset 0 0 0 1px rgba(255,255,255,0.06)"
              : "inset 0 0 0 1px rgba(255,255,255,0.05)"
            : "none",
          "&:hover": {
            transform: onClick ? "translateY(-1px)" : "none",
            backgroundColor: isSelected
              ? isBase
                ? "rgba(255,140,0,0.95)"
                : "rgba(255,170,60,0.95)"
              : "rgba(255,255,255,0.04)",
          },
        }}
      >
        <Typography
          sx={{
            fontSize: 14,
            fontWeight: 900,
            lineHeight: 1,
            color: isSelected ? "#161616" : "text.primary",
          }}
        >
          {label}
        </Typography>
      </Box>
    </Box>
  );
}

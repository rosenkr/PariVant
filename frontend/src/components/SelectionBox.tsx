import { Box, Tooltip, Typography } from "@mui/material";
import { useState } from "react";

type SelectionTone = "none" | "base" | "coverage";
type ScoreBorderTone = "none" | "static" | "live";

type Props = {
  label: string;
  tone?: SelectionTone;
  scoreBorder?: ScoreBorderTone;
  onClick?: () => void;
  disabledTooltipTitle?: string;
  showDisabledTooltipOnHover?: boolean;
};

export function SelectionBox({
  label,
  tone = "none",
  scoreBorder = "none",
  onClick,
  disabledTooltipTitle,
  showDisabledTooltipOnHover = false,
}: Props) {
  const [tooltipOpen, setTooltipOpen] = useState(false);

  const tooltipEnabled =
    showDisabledTooltipOnHover && Boolean(disabledTooltipTitle);

  return (
    <Tooltip
      title={disabledTooltipTitle ?? ""}
      open={tooltipEnabled ? tooltipOpen : false}
      disableFocusListener
      disableTouchListener
      disableInteractive
      placement="top"
    >
      <Box
        onMouseEnter={() => {
          if (tooltipEnabled) setTooltipOpen(true);
        }}
        onMouseLeave={() => {
          if (tooltipEnabled) setTooltipOpen(false);
        }}
        onClick={(e) => {
          e.stopPropagation();
          onClick?.();
        }}
        sx={(theme) => {
          const isSelected = tone !== "none";
          const isBase = tone === "base";
          const isLiveBorder = scoreBorder === "live";
          const isStaticBorder = scoreBorder === "static";

          const liveBorderSx = isLiveBorder
            ? {
                backgroundImage: `linear-gradient(
                    120deg,
                    ${theme.appColors.live.border},
                    ${theme.appColors.live.primary},
                    ${theme.appColors.live.border}
                  )`,
                backgroundSize: "220% 220%",
                animation: "scoreBorderShift 2.2s linear infinite",
                "@keyframes scoreBorderShift": {
                  "0%": { backgroundPosition: "0% 50%" },
                  "100%": { backgroundPosition: "200% 50%" },
                },
                padding: "1.5px",
                boxShadow: `0 0 0 1px ${theme.appColors.live.soft}, 0 0 18px ${theme.appColors.live.glow}`,
              }
            : null;

          const staticBorderColor = theme.appColors.live.border;
          const idleBorderColor = theme.appColors.border.strong;

          const selectedBackgroundColor = isSelected
            ? isBase
              ? theme.appColors.pick.base
              : theme.appColors.pick.coverage
            : isLiveBorder
              ? theme.appColors.surface.paper
              : "transparent";

          const hoverBackgroundColor = isSelected
            ? isBase
              ? theme.appColors.pick.base
              : theme.appColors.pick.coverage
            : isLiveBorder
              ? theme.appColors.surface.paper
              : theme.appColors.accent.soft;

          const selectedInset = isSelected
            ? isBase
              ? `inset 0 0 0 1px ${theme.appColors.pick.softBase}`
              : `inset 0 0 0 1px ${theme.appColors.pick.softCoverage}`
            : "none";

          return {
            width: 34,
            height: 34,
            borderRadius: "50%",
            cursor: onClick ? "pointer" : "default",
            userSelect: "none",
            transition: "transform 140ms ease, box-shadow 140ms ease",
            ...(liveBorderSx ?? {}),
            "& > .selection-box-inner": {
              width: "100%",
              height: "100%",
              borderRadius: "50%",
              display: "grid",
              placeItems: "center",
              border: isLiveBorder
                ? "none"
                : `1px solid ${
                    isStaticBorder ? staticBorderColor : idleBorderColor
                  }`,
              backgroundColor: selectedBackgroundColor,
              boxShadow: selectedInset,
              transition:
                "transform 140ms ease, background-color 140ms ease, border-color 140ms ease",
            },
            "&:hover > .selection-box-inner": {
              transform: onClick ? "translateY(-1px)" : "none",
              backgroundColor: hoverBackgroundColor,
            },
            "& .selection-box-label": {
              fontSize: 14,
              fontWeight: 900,
              lineHeight: 1,
              color: isSelected
                ? theme.appColors.pick.contrastText
                : theme.palette.text.primary,
            },
          };
        }}
      >
        <Box className="selection-box-inner">
          <Typography className="selection-box-label">{label}</Typography>
        </Box>
      </Box>
    </Tooltip>
  );
}

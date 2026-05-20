import { Box, Link, Typography } from "@mui/material";
import { useEffect, useMemo, useRef, useState } from "react";

export type ScrollSpySection = {
  id: string;
  title: string;
};

type ScrollSpyRailProps = {
  sections: ScrollSpySection[];
  offset?: number;
  label?: string;
  top?: number | string;
  minWidth?: number;
};

// Usage:
// <ScrollSpyRail
//   label="Round info"
//   offset={88}
//   sections={[
//     { id: "overview", title: "Overview" },
//     { id: "predictions", title: "Predictions" },
//     { id: "live", title: "Live scores" },
//   ]}
// />
//
// Note: Each target section must already have a matching DOM id, for example:
// <section id="overview">...</section>

const V_SEG = 24;
const DIAG = 10;
const X_OFF = 8;
const STROKE_W = 3;
const INITIAL_FILL = V_SEG * 0.3;

export function ScrollSpyRail({
  sections,
  offset = 80,
  label = "On this page",
  top = 96,
  minWidth = 180,
}: ScrollSpyRailProps) {
  const [activeId, setActiveId] = useState(sections[0]?.id ?? "");
  const rafRef = useRef<number | null>(null);

  const activeIndex = Math.max(
    0,
    sections.findIndex((section) => section.id === activeId),
  );

  useEffect(() => {
    if (sections.length === 0) {
      setActiveId("");
      return;
    }

    const computeActive = () => {
      const threshold = offset + 1;
      let current = sections[0].id;

      for (const section of sections) {
        const el = document.getElementById(section.id);
        if (!el) continue;

        const topPos = el.getBoundingClientRect().top;
        if (topPos <= threshold) {
          current = section.id;
        } else {
          break;
        }
      }

      setActiveId(current);
    };

    const onScroll = () => {
      if (rafRef.current !== null) return;
      rafRef.current = window.requestAnimationFrame(() => {
        computeActive();
        rafRef.current = null;
      });
    };

    computeActive();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", computeActive);

    return () => {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", computeActive);
      if (rafRef.current !== null) {
        window.cancelAnimationFrame(rafRef.current);
      }
    };
  }, [offset, sections]);

  const handleClick = (
    event: React.MouseEvent<HTMLAnchorElement>,
    id: string,
  ) => {
    event.preventDefault();
    const el = document.getElementById(id);
    if (!el) return;

    const targetTop = el.getBoundingClientRect().top + window.scrollY - offset;
    window.scrollTo({ top: targetTop, behavior: "smooth" });
  };

  const geom = useMemo(() => {
    const points: Array<{ x: number; y: number }> = [];
    let y = STROKE_W / 2;

    for (let i = 0; i < sections.length; i += 1) {
      const x = (i % 2 === 0 ? 0 : X_OFF) + STROKE_W / 2;
      if (i === 0) {
        points.push({ x, y });
      }

      y += V_SEG;
      points.push({ x, y });

      if (i < sections.length - 1) {
        const nextX = ((i + 1) % 2 === 0 ? 0 : X_OFF) + STROKE_W / 2;
        y += DIAG;
        points.push({ x: nextX, y });
      }
    }

    const segLengths: number[] = [];
    for (let i = 1; i < points.length; i += 1) {
      const dx = points[i].x - points[i - 1].x;
      const dy = points[i].y - points[i - 1].y;
      segLengths.push(Math.sqrt(dx * dx + dy * dy));
    }

    const totalLen = segLengths.reduce((sum, len) => sum + len, 0);
    const d = points
      .map((point, index) => `${index === 0 ? "M" : "L"} ${point.x} ${point.y}`)
      .join(" ");

    return {
      points,
      segLengths,
      totalLen,
      d,
      height: y + STROKE_W / 2,
      width: X_OFF + STROKE_W,
    };
  }, [sections.length]);

  const { fillLen, circle } = useMemo(() => {
    let len = 0;

    for (let i = 0; i < activeIndex; i += 1) {
      len += geom.segLengths[2 * i] ?? 0;
      len += geom.segLengths[2 * i + 1] ?? 0;
    }

    len += INITIAL_FILL;

    let remaining = len;
    let pos = geom.points[0] ?? { x: STROKE_W / 2, y: STROKE_W / 2 };

    for (let i = 1; i < geom.points.length; i += 1) {
      const segLen = geom.segLengths[i - 1];
      if (remaining <= segLen) {
        const t = segLen === 0 ? 0 : remaining / segLen;
        pos = {
          x: geom.points[i - 1].x + (geom.points[i].x - geom.points[i - 1].x) * t,
          y: geom.points[i - 1].y + (geom.points[i].y - geom.points[i - 1].y) * t,
        };
        break;
      }

      remaining -= segLen;
      pos = geom.points[i];
    }

    return { fillLen: len, circle: pos };
  }, [activeIndex, geom]);

  const labelTop = (index: number) => geom.points[2 * index]?.y ?? 0;

  if (sections.length === 0) return null;

  return (
    <Box
      component="nav"
      aria-label="Section navigation"
      sx={{
        display: { xs: "none", md: "block" },
        position: "sticky",
        top,
        alignSelf: "flex-start",
        minWidth,
      }}
    >
      <Typography
        variant="caption"
        sx={(theme) => ({
          display: "block",
          mb: 1.5,
          fontWeight: 700,
          textTransform: "uppercase",
          letterSpacing: 0.8,
          color: theme.appColors.text.secondary,
        })}
      >
        {label}
      </Typography>

      <Box sx={{ position: "relative", height: geom.height }}>
        <Box
          component="svg"
          viewBox={`0 0 ${geom.width} ${geom.height}`}
          aria-hidden="true"
          sx={{
            position: "absolute",
            left: 4,
            top: 0,
            overflow: "visible",
            width: geom.width,
            height: geom.height,
          }}
        >
          <Box
            component="path"
            d={geom.d}
            fill="none"
            stroke="currentColor"
            strokeWidth={STROKE_W}
            strokeLinecap="round"
            strokeLinejoin="round"
            sx={(theme) => ({
              color: theme.appColors.border.subtle,
            })}
          />
          <Box
            component="path"
            d={geom.d}
            fill="none"
            stroke="currentColor"
            strokeWidth={STROKE_W}
            strokeLinecap="round"
            strokeLinejoin="round"
            sx={(theme) => ({
              color: theme.appColors.text.primary,
              transition: "stroke-dashoffset 300ms ease",
              strokeDasharray: `${geom.totalLen} ${geom.totalLen}`,
              strokeDashoffset: geom.totalLen - fillLen,
            })}
          />
          <Box
            component="circle"
            cx={circle.x}
            cy={circle.y}
            r={4}
            sx={(theme) => ({
              fill: theme.appColors.text.primary,
              transition: "cx 300ms ease, cy 300ms ease",
            })}
          />
        </Box>

        <Box component="ul" sx={{ position: "relative", listStyle: "none", m: 0, p: 0 }}>
          {sections.map((section, index) => {
            const isActive = section.id === activeId;

            return (
              <Box
                component="li"
                key={section.id}
                sx={{
                  position: "absolute",
                  left: 0,
                  right: 0,
                  top: labelTop(index),
                  transform: "translateY(-3px)",
                  pl: `${geom.width + 18}px`,
                }}
              >
                <Link
                  href={`#${section.id}`}
                  underline="none"
                  onClick={(event) => handleClick(event, section.id)}
                  sx={(theme) => ({
                    display: "block",
                    whiteSpace: "nowrap",
                    fontSize: 14,
                    fontWeight: isActive ? 700 : 500,
                    color: isActive
                      ? theme.appColors.text.primary
                      : theme.appColors.text.secondary,
                    transition: "color 160ms ease",
                    "&:hover": {
                      color: theme.appColors.text.primary,
                    },
                  })}
                >
                  {section.title}
                </Link>
              </Box>
            );
          })}
        </Box>
      </Box>
    </Box>
  );
}

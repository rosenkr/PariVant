import { Box, Skeleton } from "@mui/material";

type Props = {
  rows: number;
};

export function RoundPanelSkeleton({ rows }: Props) {
  return (
    <Box>
      {/* Header bar shimmer */}
      <Box
        sx={{
          px: 2,
          py: 1.0,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          borderBottom: "1px solid rgba(255,255,255,0.08)",
        }}
      >
        <Skeleton
          variant="text"
          width={280}
          height={28}
          animation="wave"
          sx={{ bgcolor: "rgba(255,255,255,0.08)" }}
        />
        <Skeleton
          variant="text"
          width={220}
          height={22}
          animation="wave"
          sx={{ bgcolor: "rgba(255,255,255,0.08)" }}
        />
      </Box>

      {/* Match rows shimmer */}
      <Box sx={{ p: 2, display: "grid", gap: 1 }}>
        {Array.from({ length: rows }).map((_, i) => (
          <Skeleton
            key={i}
            variant="rectangular"
            height={56}
            animation="wave"
            sx={{
              borderRadius: 1,
              bgcolor:
                i % 2 === 0 ? "rgba(255,255,255,0.06)" : "rgba(255,255,255,0.04)",
            }}
          />
        ))}
      </Box>
    </Box>
  );
}
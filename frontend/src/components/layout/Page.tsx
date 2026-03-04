import { Container, Box } from "@mui/material";
import type { ReactNode } from "react";


// Contained = Home page with the current round info, full for dashboard-like pages
export type PageVariant = "contained" | "full";

type Props = {
  children: ReactNode;
  variant?: PageVariant;
  maxWidth?: "xs" | "sm" | "md" | "lg" | "xl";
  paddingY?: number;
};

// Main content
export function Page({
  children,
  variant = "contained",
  maxWidth = "lg",
  paddingY = 3,
}: Props) {
  if (variant === "full") {
    // Uncontrained width
    return (
      <Box component="main" sx={{ width: "100%", py: paddingY }}>
        {children}
      </Box>
    );
  }

  // Constrained width 
  return (
    <Container component="main" maxWidth={maxWidth} sx={{ py: paddingY }}>
      {children}
    </Container>
  );
}
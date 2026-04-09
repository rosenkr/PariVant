import { Box, Container } from "@mui/material";
import type { ContainerProps } from "@mui/material/Container";
import type { ReactNode } from "react";

type Props = {
  children: ReactNode;
  disableGutters?: boolean;
  maxWidth?: ContainerProps["maxWidth"];
};

export function Page({
  children,
  disableGutters = false,
  maxWidth = "lg",
}: Props) {
  return (
    <Box
      sx={(theme) => ({
        width: "100%",
        backgroundColor: theme.appColors.surface.page,
        pt: { xs: 2, md: 3 },
        pb: { xs: 4, md: 6 },
      })}
    >
      <Container maxWidth={maxWidth} disableGutters={disableGutters}>
        {children}
      </Container>
    </Box>
  );
}

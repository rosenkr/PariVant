import { Box, Container } from "@mui/material";
import type { ReactNode } from "react";

type Props = {
  children: ReactNode;
  disableGutters?: boolean;
};

export function Page({ children, disableGutters = false }: Props) {
  return (
    <Box
      component="main"
      sx={{
        width: "100%",
        py: { xs: 2, md: 3 },
      }}
    >
      <Container maxWidth="lg" disableGutters={disableGutters}>
        {children}
      </Container>
    </Box>
  );
}
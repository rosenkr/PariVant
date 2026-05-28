import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import {
  Box,
  Dialog,
  DialogContent,
  IconButton,
  Stack,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import type { AuthView } from "../../types/auth";
import { LoginForm } from "./LoginForm";
import { RegisterForm } from "./RegisterForm";

type Props = {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
};

export function AuthModal({ open, onClose, onSuccess }: Props) {
  const [view, setView] = useState<AuthView>("sign-in");

  useEffect(() => {
    if (!open) {
      setView("sign-in");
    }
  }, [open]);

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="xs"
      scroll="body"
      slotProps={{
        paper: {
          sx: (theme) => ({
            borderRadius: 3,
            backgroundColor: theme.appColors.auth.modalBackground,
            border: `1px solid ${theme.appColors.auth.modalBorder}`,
            boxShadow: theme.appColors.auth.modalShadow,
          }),
        },
      }}
    >
      <DialogContent
        sx={{
          p: { xs: 3, sm: 4 },
        }}
      >
        <Stack spacing={3}>
          <Stack spacing={2} alignItems="center" sx={{ position: "relative" }}>
            <IconButton
              onClick={onClose}
              aria-label="Close authentication dialog"
              sx={{
                position: "absolute",
                top: -8,
                right: -8,
              }}
            >
              <CloseRoundedIcon />
            </IconButton>

            <Box
              component="img"
              src="/favicon.svg"
              alt="PariVant logo"
              sx={{
                width: 52,
                height: 52,
                display: "block",
              }}
            />

            <Stack spacing={0.5} alignItems="center">
              <Typography variant="h5" align="center" sx={{ fontWeight: 800 }}>
                Welcome to PariVant
              </Typography>
              <Typography
                variant="body1"
                align="center"
                sx={(theme) => ({
                  color: theme.appColors.text.secondary,
                })}
              >
                Make more informed decisions
              </Typography>
            </Stack>
          </Stack>

          {view === "sign-in" && (
            <LoginForm
              onSwitchToSignUp={() => setView("sign-up")}
              onSuccess={onSuccess ?? onClose}
            />
          )}

          {view === "sign-up" && (
            <RegisterForm
              onSwitchToSignIn={() => setView("sign-in")}
              onSuccess={onSuccess ?? onClose}
            />
          )}
        </Stack>
      </DialogContent>
    </Dialog>
  );
}

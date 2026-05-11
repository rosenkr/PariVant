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
import { VerifyEmailView } from "./VerifyEmailView";

type Props = {
  open: boolean;
  onClose: () => void;
};

export function AuthModal({ open, onClose }: Props) {
  const [view, setView] = useState<AuthView>("sign-in");
  const [verificationEmail, setVerificationEmail] = useState("");
  const [verificationExpiresInSeconds, setVerificationExpiresInSeconds] =
    useState(10 * 60);

  useEffect(() => {
    if (!open) {
      setView("sign-in");
      setVerificationEmail("");
      setVerificationExpiresInSeconds(10 * 60);
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
              onSuccess={onClose}
            />
          )}

          {view === "sign-up" && (
            <RegisterForm
              onSwitchToSignIn={() => setView("sign-in")}
              onRegistered={(email, verificationExpiresInSecondsValue) => {
                setVerificationEmail(email);
                setVerificationExpiresInSeconds(
                  verificationExpiresInSecondsValue,
                );
                setView("verify-email");
              }}
            />
          )}

          {view === "verify-email" && (
            <VerifyEmailView
              email={verificationEmail}
              expiresInSeconds={verificationExpiresInSeconds}
              onBackToSignUp={() => setView("sign-up")}
              onVerified={onClose}
            />
          )}
        </Stack>
      </DialogContent>
    </Dialog>
  );
}

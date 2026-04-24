import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ReplayRoundedIcon from "@mui/icons-material/ReplayRounded";
import {
  Alert,
  Button,
  CircularProgress,
  Link,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import type { FormEventHandler } from "react";
import { resendVerificationCode, verifyEmailCode } from "../../api/auth";

type Props = {
  email: string;
  expiresInSeconds: number;
  onBackToSignUp: () => void;
  onVerified: () => void;
};

function formatCooldown(secondsRemaining: number) {
  if (secondsRemaining <= 0) return "Resend code";
  return `Resend in ${secondsRemaining}s`;
}

function formatExpiry(secondsRemaining: number) {
  if (secondsRemaining <= 0) return "Code expired";
  const minutes = Math.floor(secondsRemaining / 60);
  const seconds = secondsRemaining % 60;
  return `Code expires in ${minutes}:${String(seconds).padStart(2, "0")}`;
}

export function VerifyEmailView({
  email,
  expiresInSeconds,
  onBackToSignUp,
  onVerified,
}: Props) {
  const [code, setCode] = useState("");
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitInfo, setSubmitInfo] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(30);
  const [expiryCountdown, setExpiryCountdown] = useState(expiresInSeconds);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setResendCooldown((current) => Math.max(current - 1, 0));
      setExpiryCountdown((current) => Math.max(current - 1, 0));
    }, 1000);

    return () => window.clearInterval(timer);
  }, []);

  const codeError = useMemo(() => {
    if (!code) return "";
    if (!/^\d{0,6}$/.test(code)) return "Only digits are allowed.";
    if (code.length < 6) return "Enter the 6-digit code.";
    return "";
  }, [code]);

  const handleVerify: FormEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    setSubmitError(null);
    setSubmitInfo(null);

    if (!/^\d{6}$/.test(code)) {
      setSubmitError("Enter the 6-digit verification code.");
      return;
    }

    setIsVerifying(true);

    void (async () => {
      try {
        await verifyEmailCode({ email, code });
        onVerified();
      } catch (error) {
        setSubmitError(
          error instanceof Error ? error.message : "Unable to verify this code."
        );
      } finally {
        setIsVerifying(false);
      }
    })();
  };

  const handleResend = async () => {
    if (resendCooldown > 0) return;

    setSubmitError(null);
    setSubmitInfo(null);
    setIsResending(true);

    try {
      await resendVerificationCode(email);
      setResendCooldown(30);
      setExpiryCountdown(expiresInSeconds);
      setSubmitInfo(`A new code has been sent to ${email}.`);
    } catch {
      setSubmitError("Could not resend the code. Please try again.");
    } finally {
      setIsResending(false);
    }
  };

  return (
    <Stack spacing={2.25} component="form" onSubmit={handleVerify} noValidate>
      <Stack spacing={0.75}>
        <Typography variant="body1" align="center">
          We sent a 6-digit verification code to
        </Typography>
        <Typography variant="subtitle1" align="center" sx={{ fontWeight: 700 }}>
          {email}
        </Typography>
      </Stack>

      {(submitError || expiryCountdown === 0) && (
        <Alert severity="error">
          {submitError ?? "This verification code has expired. Please resend it."}
        </Alert>
      )}

      {submitInfo && <Alert severity="success">{submitInfo}</Alert>}

      <TextField
        label="Verification code"
        value={code}
        onChange={(event) =>
          setCode(event.target.value.replace(/\D/g, "").slice(0, 6))
        }
        error={Boolean(codeError)}
        helperText={codeError || formatExpiry(expiryCountdown)}
        slotProps={{
          htmlInput: {
            inputMode: "numeric",
            autoComplete: "one-time-code",
            maxLength: 6,
            "aria-label": "6-digit email verification code",
            style: {
              letterSpacing: "0.6em",
              textAlign: "center",
              fontVariantNumeric: "tabular-nums",
            },
          },
        }}
        fullWidth
      />

      <Button
        type="submit"
        variant="contained"
        disabled={isVerifying || code.length !== 6}
        startIcon={
          isVerifying ? <CircularProgress size={18} color="inherit" /> : null
        }
      >
        {isVerifying ? "Verifying..." : "Verify email"}
      </Button>

      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={1}
        justifyContent="space-between"
        alignItems={{ xs: "stretch", sm: "center" }}
      >
        <Button
          type="button"
          onClick={() => {
            void handleResend();
          }}
          disabled={isResending || resendCooldown > 0}
          startIcon={
            isResending ? (
              <CircularProgress size={18} color="inherit" />
            ) : (
              <ReplayRoundedIcon />
            )
          }
        >
          {isResending ? "Sending..." : formatCooldown(resendCooldown)}
        </Button>

        <Link
          component="button"
          type="button"
          underline="hover"
          onClick={onBackToSignUp}
          sx={{
            display: "inline-flex",
            alignItems: "center",
            gap: 0.75,
          }}
        >
          <EditOutlinedIcon sx={{ fontSize: 16 }} />
          Change email
        </Link>
      </Stack>
    </Stack>
  );
}
import VisibilityOffRoundedIcon from "@mui/icons-material/VisibilityOffRounded";
import VisibilityRoundedIcon from "@mui/icons-material/VisibilityRounded";
import {
  Alert,
  Button,
  CircularProgress,
  IconButton,
  InputAdornment,
  Link,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMemo, useState } from "react";
import type { FormEventHandler } from "react";
import { registerAccount } from "../../api/auth";
import { PasswordStrengthMeter } from "./PasswordStrengthMeter";
import {useAuth} from "../../auth/AuthContext.tsx";

type Props = {
  onSwitchToSignIn: () => void;
  onSuccess: () => void;
};

function validateEmail(email: string) {
  return /\S+@\S+\.\S+/.test(email);
}

export function RegisterForm({ onSwitchToSignIn, onSuccess}: Props) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [emailTouched, setEmailTouched] = useState(false);
  const [passwordTouched, setPasswordTouched] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { signIn } = useAuth();
  const emailError = useMemo(() => {
    if (!emailTouched) return "";
    if (!email.trim()) return "Email is required.";
    if (!validateEmail(email)) return "Enter a valid email address.";
    return "";
  }, [email, emailTouched]);

  const passwordError = useMemo(() => {
    if (!passwordTouched) return "";
    if (!password) return "Password is required.";
    if (password.length < 8) return "Password must be at least 8 characters.";
    return "";
  }, [password, passwordTouched]);

  const hasValidationErrors = Boolean(emailError || passwordError);

  const handleSubmit: FormEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    setEmailTouched(true);
    setPasswordTouched(true);
    setSubmitError(null);

    if (!email.trim() || !password || !validateEmail(email) || password.length < 8) {
      return;
    }

    setIsSubmitting(true);

    void (async () => {
      try {
        const authResponse = await registerAccount({ email, password });
        signIn(authResponse);
        onSuccess();
      } catch (error) {
        setSubmitError(
          error instanceof Error ? error.message : "Unable to create account right now."
        );
      } finally {
        setIsSubmitting(false);
      }
    })();
  };

  return (
    <Stack spacing={2.25} component="form" onSubmit={handleSubmit} noValidate>
      {submitError && <Alert severity="error">{submitError}</Alert>}

      <TextField
        label="Email"
        type="email"
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        onBlur={() => setEmailTouched(true)}
        autoComplete="email"
        error={Boolean(emailError)}
        helperText={emailError || " "}
        fullWidth
      />

      <TextField
        label="Password"
        type={showPassword ? "text" : "password"}
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        onBlur={() => setPasswordTouched(true)}
        autoComplete="new-password"
        error={Boolean(passwordError)}
        helperText={passwordError || " "}
        fullWidth
        slotProps={{
          input: {
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  edge="end"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  onClick={() => setShowPassword((current) => !current)}
                >
                  {showPassword ? (
                    <VisibilityOffRoundedIcon />
                  ) : (
                    <VisibilityRoundedIcon />
                  )}
                </IconButton>
              </InputAdornment>
            ),
          },
        }}
      />

      <PasswordStrengthMeter password={password} />

      <Button
        type="submit"
        variant="contained"
        disabled={isSubmitting || hasValidationErrors}
        startIcon={
          isSubmitting ? <CircularProgress size={18} color="inherit" /> : null
        }
      >
        {isSubmitting ? "Creating account..." : "Create account"}
      </Button>

      <Typography
        variant="body2"
        align="center"
        sx={(theme) => ({
          color: theme.appColors.text.secondary,
        })}
      >
        Already a member?{" "}
        <Link component="button" type="button" underline="hover" onClick={onSwitchToSignIn}>
          Log in
        </Link>
      </Typography>
    </Stack>
  );
}
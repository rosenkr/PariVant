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
import { GoogleSignInButton } from "./GoogleSignInButton";
import { useAuth} from "../../auth/AuthContext.tsx";
import {signInWithEmailAndPw} from "../../api/auth.ts";

type Props = {
  onSwitchToSignUp: () => void;
  onSuccess: () => void;
};

function validateEmail(email: string) {
  return /\S+@\S+\.\S+/.test(email);
}

export function LoginForm({ onSwitchToSignUp, onSuccess }: Props) {
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
    return "";
  }, [password, passwordTouched]);

  const hasValidationErrors = Boolean(emailError || passwordError);

  const handleSubmit: FormEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    setEmailTouched(true);
    setPasswordTouched(true);
    setSubmitError(null);

    if (!email.trim() || !password || !validateEmail(email)) {
      return;
    }

    setIsSubmitting(true);

    void (async () => {
      try {
        const authResponse = await signInWithEmailAndPw({ email, password });
        signIn(authResponse);
        onSuccess();
      } catch (error) {
        setSubmitError(
          error instanceof Error ? error.message : "Unable to sign in right now."
        );
      } finally {
        setIsSubmitting(false);
      }
    })();
  };

  const handleForgotPassword = () => {
    setSubmitError("Password reset is not implemented yet.");
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
        autoComplete="current-password"
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

      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        spacing={2}
      >
        <Link
          component="button"
          type="button"
          onClick={handleForgotPassword}
          underline="hover"
          sx={{ alignSelf: "flex-start" }}
        >
          Forgot password?
        </Link>

        <Button
          type="submit"
          variant="contained"
          disabled={isSubmitting || hasValidationErrors}
          startIcon={
            isSubmitting ? <CircularProgress size={18} color="inherit" /> : null
          }
          sx={{ minWidth: 152 }}
        >
          {isSubmitting ? "Signing in..." : "Sign in"}
        </Button>
      </Stack>

      <Stack spacing={1.5}>
        <Typography
          variant="body2"
          align="center"
          sx={(theme) => ({
            color: theme.appColors.text.secondary,
          })}
        >
          OR
        </Typography>

        <GoogleSignInButton
          onStart={() => {
            setIsSubmitting(true);
            setSubmitError(null);
          }}
          onSuccess={(authResponse) => {
            signIn(authResponse);
            setIsSubmitting(false);
            onSuccess();
          }}
          onError={(message) => {
            setIsSubmitting(false);
            setSubmitError(message);
          }}
        />
      </Stack>

      <Typography
        variant="body2"
        align="center"
        sx={(theme) => ({
          color: theme.appColors.text.secondary,
        })}
      >
        Not on PariVant yet?{" "}
        <Link component="button" type="button" underline="hover" onClick={onSwitchToSignUp}>
          Sign up
        </Link>
      </Typography>
    </Stack>
  );
}
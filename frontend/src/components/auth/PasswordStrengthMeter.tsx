import { Box, Stack, Typography } from "@mui/material";

type Props = {
  password: string;
};

function scorePassword(password: string) {
  let score = 0;

  if (password.length >= 8) score += 1;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score += 1;
  if (/\d/.test(password)) score += 1;
  if (/[^A-Za-z0-9]/.test(password)) score += 1;

  return Math.min(score, 4);
}

function getStrengthLabel(score: number) {
  if (score <= 1) return "Weak";
  if (score <= 2) return "Fair";
  if (score === 3) return "Good";
  return "Strong";
}

export function PasswordStrengthMeter({ password }: Props) {
  const score = scorePassword(password);
  const filledBars = Math.max(score, password.length > 0 ? 1 : 0);
  const label = getStrengthLabel(score);

  return (
    <Stack spacing={0.75}>
      <Stack direction="row" spacing={0.75}>
        {[0, 1, 2, 3].map((index) => (
          <Box
            key={index}
            sx={(theme) => {
              const active = index < filledBars;

              let activeColor = theme.appColors.auth.strengthWeak;
              if (score >= 3) activeColor = theme.appColors.auth.strengthMedium;
              if (score >= 4) activeColor = theme.appColors.auth.strengthStrong;

              return {
                flex: 1,
                height: 6,
                borderRadius: 999,
                backgroundColor: active
                  ? activeColor
                  : theme.appColors.auth.strengthTrack,
                transition: "background-color 160ms ease",
              };
            }}
          />
        ))}
      </Stack>

      <Typography
        variant="caption"
        sx={(theme) => ({
          color: theme.appColors.text.secondary,
        })}
      >
        Password strength: {label}. Minimum 8 characters.
      </Typography>
    </Stack>
  );
}
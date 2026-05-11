export type AuthView = "sign-in" | "sign-up" | "verify-email";

export type AuthErrorCode =
  | "INVALID_CREDENTIALS"
  | "EMAIL_ALREADY_IN_USE"
  | "WEAK_PASSWORD"
  | "INVALID_CODE"
  | "EXPIRED_CODE"
  | "UNKNOWN";

export type AuthError = {
  code: AuthErrorCode;
  message: string;
};

export type SignInPayload = {
  email: string;
  password: string;
};

export type RegisterPayload = {
  email: string;
  password: string;
};

export type VerifyEmailPayload = {
  email: string;
  code: string;
};

export type GoogleAuthRequest = {
  credential: string;
};

export type RegisterResponse = {
  email: string;
  verificationExpiresInSeconds: number;
};

export type VerifyEmailResponse = {
  success: true;
};

export type AuthResponse = {
  token: string;
  user: {
    email: string;
    role: string;
  };
};
export type AuthView = "sign-in" | "sign-up";

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

export type AuthRequest = {
  email: string;
  password: string;
};

export type GoogleAuthRequest = {
  credential: string;
};

export type AuthResponse = {
  token: string;
  user: {
    email: string;
    role: string;
  };
};
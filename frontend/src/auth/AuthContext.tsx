import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import type { AuthResponse } from "../types/auth";

type AuthContextValue = {
  token: string | null;
  user: AuthResponse["user"] | null;
  isAuthenticated: boolean;
  signIn: (authResponse: AuthResponse) => void;
  signOut: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);
const AUTH_STORAGE_KEY = "parivant.auth";

function readStoredAuth(): AuthResponse | null {
  if (typeof window === "undefined") return null;

  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return null;

  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [storedAuth] = useState<AuthResponse | null>(() => readStoredAuth());
  const [token, setToken] = useState<string | null>(storedAuth?.token ?? null);
  const [user, setUser] = useState<AuthResponse["user"] | null>(
    storedAuth?.user ?? null,
  );

  // Updates token + user
  const signIn = useCallback((authResponse: AuthResponse) => {
    setToken(authResponse.token);
    setUser(authResponse.user);
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authResponse));
  }, []);

  const signOut = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isAuthenticated: token !== null,
      signIn,
      signOut,
    }),
    [signIn, signOut, token, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider.");
  }

  return context;
}

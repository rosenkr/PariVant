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

export function AuthProvider({ children }: PropsWithChildren) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthResponse["user"] | null>(null);

  // Updates token + user
  const signIn = useCallback((authResponse: AuthResponse) => {
    setToken(authResponse.token);
    setUser(authResponse.user);
  }, []);

  const signOut = useCallback(() => {
    setToken(null);
    setUser(null);
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

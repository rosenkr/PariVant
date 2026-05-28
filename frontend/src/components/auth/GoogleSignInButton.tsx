import { Alert, Box, CircularProgress, Stack, Typography } from "@mui/material";
import { useEffect, useMemo, useRef, useState } from "react";
import { signInWithGoogle } from "../../api/auth";
import type { AuthResponse } from "../../types/auth";

declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: {
          initialize: (options: {
            client_id: string;
            callback: (response: { credential: string }) => void;
          }) => void;
          renderButton: (
            element: HTMLElement,
            options: Record<string, unknown>,
          ) => void;
          prompt: () => void;
        };
      };
    };
  }
}

type Props = {
  onStart: () => void;
  onSuccess: (authResponse: AuthResponse) => void;
  onError: (message: string) => void;
};

const GIS_SCRIPT_ID = "parivant-gis-script";

function ensureGoogleScript() {
  return new Promise<void>((resolve, reject) => {
    const existing = document.getElementById(
      GIS_SCRIPT_ID,
    ) as HTMLScriptElement | null;

    if (existing) {
      if (window.google?.accounts?.id) {
        resolve();
        return;
      }

      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener(
        "error",
        () => reject(new Error("Failed to load Google Identity Services.")),
        { once: true },
      );
      return;
    }

    const script = document.createElement("script");
    script.id = GIS_SCRIPT_ID;
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () =>
      reject(new Error("Failed to load Google Identity Services."));
    document.head.appendChild(script);
  });
}

export function GoogleSignInButton({ onStart, onSuccess, onError }: Props) {
  const gisContainerRef = useRef<HTMLDivElement | null>(null);
  const onStartRef = useRef(onStart);
  const onSuccessRef = useRef(onSuccess);
  const onErrorRef = useRef(onError);
  const [gisReady, setGisReady] = useState(false);
  const [gisError, setGisError] = useState<string | null>(null);

  const clientId = useMemo(() => {
    return typeof import.meta.env.VITE_GOOGLE_CLIENT_ID === "string"
      ? import.meta.env.VITE_GOOGLE_CLIENT_ID
      : undefined;
  }, []);

  useEffect(() => {
    onStartRef.current = onStart;
    onSuccessRef.current = onSuccess;
    onErrorRef.current = onError;
  }, [onError, onStart, onSuccess]);

  useEffect(() => {
    if (!clientId) {
      setGisError("Google sign-in is not configured.");
      setGisReady(false);
      return;
    }

    if (!gisContainerRef.current) {
      return;
    }

    let active = true;

    setGisError(null);
    setGisReady(false);

    void ensureGoogleScript()
      .then(() => {
        if (
          !active ||
          !window.google?.accounts?.id ||
          !gisContainerRef.current
        ) {
          return;
        }

        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: ({ credential }) => {
            void (async () => {
              onStartRef.current();

              try {
                const authResponse = await signInWithGoogle({ credential });
                onSuccessRef.current(authResponse);
              } catch {
                onErrorRef.current(
                  "Google sign-in is not available yet. Please try email sign-in.",
                );
              }
            })();
          },
        });

        gisContainerRef.current.innerHTML = "";
        window.google.accounts.id.renderButton(gisContainerRef.current, {
          theme: "outline",
          size: "large",
          shape: "pill",
          width: 320,
          text: "continue_with",
        });

        setGisReady(true);
      })
      .catch(() => {
        if (!active) return;
        setGisError("Could not load Google sign-in.");
        setGisReady(false);
      });

    return () => {
      active = false;
      if (gisContainerRef.current) {
        gisContainerRef.current.innerHTML = "";
      }
    };
  }, [clientId]);

  return (
    <Stack spacing={1.25}>
      {!gisReady && !gisError && (
        <Stack
          direction="row"
          spacing={1}
          alignItems="center"
          justifyContent="center"
          sx={{ height: 44 }}
        >
          <CircularProgress size={18} />
          <Typography variant="body2">Loading Google sign-in…</Typography>
        </Stack>
      )}

      <Box
        ref={gisContainerRef}
        sx={{
          height: 44,
          width: 320,
          mx: "auto",
          overflow: "hidden",
          display: gisError ? "none" : "flex",
          justifyContent: "center",
          alignItems: "center",
        }}
      />

      {gisError && <Alert severity="warning">{gisError}</Alert>}
    </Stack>
  );
}

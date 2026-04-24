2. Google sign-in is only half-real

In src/components/auth/GoogleSignInButton.tsx:

the GIS script is already being loaded dynamically
client_id is already taken from VITE_GOOGLE_CLIENT_ID
window.google.accounts.id.initialize(...) is already set up
renderButton(...) is already used

So this part is actually in decent shape.

But in src/api/auth.ts:

export async function signInWithGoogle(...) {
try {
await postJson("/api/auth/google", payload);
} catch {
await delay(PLACEHOLDER_DELAY_MS);
}
}

That means:

it tries to POST to /api/auth/google
if it fails, it silently falls back to fake success timing

So backend verification is still placeholder-ish.

If you want this to become real, the fallback catch { await delay(...) } needs to go
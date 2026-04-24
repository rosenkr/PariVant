Auth modal flow: current placeholders
1. Email/password auth is still fully mocked

In src/api/auth.ts:

signIn() does not call backend
registerAccount() does not call backend
verifyEmailCode() does not call backend
resendVerificationCode() does not call backend

Current behavior is placeholder logic:

sign-in fails only if email contains "fail"
registration fails only if email contains "taken"
verification succeeds only for code "123456"
code "000000" is treated as expired
resend just waits briefly

So the main placeholder area is:

await delay(...)

instead of real fetch(...) calls.

2. No authenticated session state exists yet

Right now the modal closes on “success”, but there is no actual frontend auth state.

That means:

no current user
no token storage
no cookie/session handling awareness
no navbar state change after login
no protected route behavior
no “My Page” route yet

So currently auth is only a UI flow, not an app-wide signed-in state.
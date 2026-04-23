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
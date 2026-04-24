Email verification is UI-complete but backend-placeholder

VerifyEmailView.tsx is a good UI scaffold:

code input
resend cooldown
expiration countdown
invalid/expired states
change email action

But the actual semantics are still fake because verifyEmailCode() and resendVerificationCode() are mocked in src/api/auth.ts.

Also:
“Change email” in verification only goes back to sign-up

That currently means:

user is sent back to sign-up view
previous email is not preserved into an editable field automatically
it is a UI navigation convenience, not a full edit-email flow

This may be acceptable for now, but it is worth knowing
Auth Modal Flow

Entry point
Open a modal popup on the homepage when the user clicks the navbar profile icon.
The modal should feel similar to Pinterest’s login/signup modal.

Default views
Open with the Sign in form by default.
Include a toggle link:
“Not on PariVant yet? Sign up” → switches to the registration form
“Already a member? Log in” → switches back to the sign-in form

Modal header
Show the PariVant icon at the top.
Header text:
Welcome to PariVant
Make more informed decisions

Authentication Options

Email is the primary login identifier.
Username/display name is out of scope for now.
Include Google as an alternative auth option:
OR Continue with Google

UX Requirements

Selected/focused field should have a highlighted border.
Password field should include an eye icon for toggling visibility.
Show a password strength meter with:
Minimum password length: 8
Any color used should not be hardcoded; use the theme folder.
If possible/applicable, use existing MUI components.
On submit, button text should change to:
Signing in...
Creating account...
Should support Firefox’s “Use securely generated password?” behavior when the user clicks the password field.

Google Sign-In

Implement the GIS button.
Add an async placeholder POST to:
/api/auth/google
Backend verification is deferred for now.

Validation

Use basic client-side validation for UX only.
Backend validation remains the source of truth.
Send a verification email with a 6-digit code for semantic validation.

Async Submit Behavior

We are not doing the backend part yet, but submitting should use async code with placeholders for now.

While the async request is in progress, the UI should show:
loading spinner
disabled submit button
error message if request fails

Password Reset

Include “Forgot password?” on the sign-in form.
Password reset flow is not implemented yet, but leave a placeholder route/action.

Email Verification Flow

After successful registration submit, switch to a verify-email view.
Verification screen should include:
6 inputs, or one input styled for 6 digits
Include resend code action with cooldown.
Allow changing email from the verification step.
Show expiration / invalid-code states.
Do not fully activate the account until verification succeeds.

Security / Transport

All auth-related requests assume HTTPS only.

# UI Tech Choices (MVP)

## Component library: Material UI (MUI)

Decision:
- Use Material UI (MUI) for the initial React UI.

Why:
- Good defaults for typography, spacing, responsiveness, accessibility.
- Faster to build a clean UI as a beginner (less CSS decision overhead).
- Works well with TypeScript and Vite.

Scope:
- Use MUI components for:
    - App layout (containers, cards, grid/stack)
    - Controls (dropdowns, buttons)
    - Basic feedback (loading, error banners)

Non-goals (for now):
- No custom design system.
- No Svenska Spel Design System code reuse (license restricted).

Install (frontend):
- `npm install @mui/material @emotion/react @emotion/styled`
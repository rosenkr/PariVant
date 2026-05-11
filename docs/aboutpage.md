0. add a railway-like thing that when you scroll down
its a filled object that is pulled along a rail which looks nice


UI: Do it like 11elo:
1. all content is centered
2. All text is in cards with subcards
3. Card1: What is Parivant? Motivation behind this project ---> Hero
4. Card2: Model V1. Value. Balance when covering, matches intuition of human mind. Texbox for receiving feedback and tips on improving the model concept
5. Mathematical formulas (weighted KL, ensemble)
6. Data sources
7. Some closing section ---> Call to action

Consider structure of AppShell:
Available width — Check whether the About page has the full screen width or is inside a layout with a sidebar/navbar.
Parent padding — See if the parent already adds padding, so the About page does not add too much extra spacing.
Background color — Decide whether the background should be controlled by the parent layout or by the About page itself.
Height behavior — Make sure the page height works correctly inside the app shell and does not rely on 100% unless the parent supports it.
Scroll behavior — Check whether scrolling happens on the whole page or inside a specific layout container.
Page responsibility — Let the About page handle its own sections, cards, text, and spacing.
Parent responsibility — Let the parent handle global layout concerns like sidebar, navbar, overall page frame, and route placement.

MUI Components probably usable:
Box
Container
Stack
Grid
Divider
Card
CardContent
Paper
Typography
List
ListItem
ListItemText
ListItemIcon
@mui/icons-material

GPT structure idea:
Whole page background        → Box
Centered content width       → Container
Each big about section       → Paper
Section header row           → Stack + icon + Typography
Paragraph text               → Typography
Formula group container      → Paper or Box
Individual formula boxes     → Card
Formula text                 → Typography component="pre"
Parameters list              → List or Stack
Tags                         → Chip
Call-to-action links/buttons → Button

